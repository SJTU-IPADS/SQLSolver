package sqlsolver.superopt.liastar.translator;

import static sqlsolver.superopt.liastar.translator.BvmEvaluation.diffBetweenVarOccurrencesInTerms;
import static sqlsolver.superopt.liastar.translator.LiaStarTranslatorSupport.collectRelatedSubTermSet;
import static sqlsolver.superopt.liastar.translator.LiaStarTranslatorSupport.getOutermostMultiArgOrAtomTerm;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import sqlsolver.common.utils.NameSequence;
import sqlsolver.superopt.uexpr.*;

/** Select one bound variable from each summation so that they match well. */
public class HeuristicBoundVarMatcher {

  public record Result(UVar newCommonVar, Set<UVar> replacedBoundVars) {}

  private enum MatchingMode {
    DIFF,
    PATTERN,
    FORCE
  }

  private final NameSequence boundVarName;
  private final UVar outVar;
  private List<UTerm> sums;
  private Set<Integer> sumIndexSet1;
  private Set<Integer> sumIndexSet2;
  private Map<Integer, UTerm> previousSums;

  private record CommonTupleResult(UVar tuple, String table, int appearsIn) {
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof CommonTupleResult that) {
        // irrelevant to position
        return tuple.equals(that.tuple) && table.equals(that.table);
      }
      return false;
    }
  }

  /** Find all possible unique common tuples. */
  private Set<CommonTupleResult> getCommonTuples() {
    Set<CommonTupleResult> commons = new HashSet<>();
    for (int i = 0; i < sums.size(); ++i) {
      USum curSum = ((USum) sums.get(i));
      for (UVar v : curSum.boundedVars()) {
        String commonTable = findTableForTuple(curSum, v);
        commons.add(new CommonTupleResult(v, commonTable, i));
      }
    }
    return commons;
  }

  private String findTableForTuple(UTerm expr, UVar tuple) {
    switch (expr.kind()) {
      case ADD, MULTIPLY -> {
        for (UTerm term : expr.subTerms()) {
          String tmp = findTableForTuple(term, tuple);
          if (tmp != null) {
            return tmp;
          }
        }
        return null;
      }
      case SQUASH -> {
        return findTableForTuple(((USquash) expr).body(), tuple);
      }
      case NEGATION -> {
        return findTableForTuple(((UNeg) expr).body(), tuple);
      }
      case SUMMATION -> {
        return findTableForTuple(((USum) expr).body(), tuple);
      }
      case TABLE -> {
        UTable tmp = (UTable) expr;
        if (tmp.var().equals(tuple)) {
          return tmp.tableName().toString();
        } else {
          return null;
        }
      }
      default -> {
        return null;
      }
    }
  }

  private UTerm replaceTargetTupleFromOneSum(
      USum cur, UVar v, UVar commonTupleNewName, ArrayList<UVar> preVars) {

    cur.boundedVars().remove(v);
    cur.replaceVarInplace(v, commonTupleNewName, true);
    if (cur.boundedVars().isEmpty()) {
      preVars.add(v);
      return cur.body().copy();
    } else {
      preVars.add(v);
      return cur.copy();
    }
  }

  private Set<UVar> getCandidateVars(USum cur, String commonTable) {

    Set<UVar> boundTuples = cur.boundedVars();
    Set<UVar> candidateVars = new HashSet<>();
    for (UVar v : boundTuples) {
      String tableName = findTableForTuple(cur.body(), v);
      if (tableName == null) {
        continue;
      }
      if (tableName.equals(commonTable)) {
        candidateVars.add(v);
      }
    }
    return candidateVars;
  }

  /** The minimum of difference between "tuple" in "term" and each term in "baseline". */
  private int computeDiff(UVar tuple, UTerm term, Set<UTerm> baseline) {
    if (baseline.isEmpty()) return Integer.MAX_VALUE;
    return baseline.stream()
        .map(s -> diffBetweenVarOccurrencesInTerms(tuple, term, s))
        .min(Integer::compareTo)
        .get();
  }

  private UTerm findCorrespondingSummation(USum sum, Set<UTerm> baseline) {
    int bvCount = sum.boundedVars().size();
    for (UTerm base : baseline) {
      if (base instanceof USum sum0) {
        int bvCount0 = sum0.boundedVars().size();
        if (bvCount == bvCount0 + 1) {
          // one bound var in sum0 was replaced
          // so sum has one more bound var than sum0
          return base;
        }
      }
    }
    return null;
  }

  // find "a" such that [a(x) = b(...)] equals "term"
  private UName getAttrRelatedToOutVar(UTerm term) {
    if (term instanceof UPred pred && pred.isPredKind(UPred.PredKind.EQ)) {
      UTerm left = pred.args().get(0);
      UTerm right = pred.args().get(1);
      if (left instanceof UVarTerm vt1 && right instanceof UVarTerm vt2) {
        UVar v1 = vt1.var();
        UVar v2 = vt2.var();
        // "term" is like [a(x) = b(...)]
        if (v1.kind() == UVar.VarKind.PROJ && v2.kind() == UVar.VarKind.PROJ) {
          UVar[] args1 = v1.args();
          UVar[] args2 = v2.args();
          if (args1.length == 1 && args1[0].equals(outVar)) {
            return v1.name(); // a
          }
          if (args2.length == 1 && args2[0].equals(outVar)) {
            return v2.name(); // a
          }
        }
      }
    }
    return null;
  }

  // find xN such that [a(x) = b(xN)] equals "term"
  private UVar getVarRelatedToOutVar(UTerm term, UName attr) {
    if (term instanceof UPred pred && pred.isPredKind(UPred.PredKind.EQ)) {
      UTerm left = pred.args().get(0);
      UTerm right = pred.args().get(1);
      if (left instanceof UVarTerm vt1 && right instanceof UVarTerm vt2) {
        UVar v1 = vt1.var();
        UVar v2 = vt2.var();
        // "term" is like [a(x) = b(xN)]
        if (v1.kind() == UVar.VarKind.PROJ && v2.kind() == UVar.VarKind.PROJ) {
          UVar[] args1 = v1.args();
          UVar[] args2 = v2.args();
          if (args1.length == 1
              && args1[0].equals(outVar)
              && v1.name().equals(attr)
              && args2.length == 1) {
            return args2[0]; // xN
          }
          if (args2.length == 1
              && args2[0].equals(outVar)
              && v2.name().equals(attr)
              && args1.length == 1) {
            return args1[0]; // xN
          }
        }
      }
    }
    return null;
  }

  private List<UVar> collectTableVarList(UTerm term, UName tableName) {
    List<UVar> vars = new ArrayList<>();
    for (UTerm subTerm : term.subTerms()) {
      if (subTerm instanceof UTable tableTerm && tableTerm.tableName().equals(tableName)) {
        vars.add(tableTerm.var());
      }
    }
    return vars;
  }

  private UVar chooseAmongMultiBests(
      int curIndex, String commonTable, UVar commonTupleNewName, Set<UTerm> baseline) {
    USum cur = (USum) sums.get(curIndex).copy();
    USum partner = (USum) findCorrespondingSummation(cur, baseline);

    if (partner == null) return null;
    partner = (USum) partner.copy();

    UTerm t1 = getOutermostMultiArgOrAtomTerm(partner.body());
    UTerm t2 = getOutermostMultiArgOrAtomTerm(cur.body());
    Set<UVar> bvs1 = partner.boundedVars();
    Set<UVar> bvs2 = cur.boundedVars();

    // if [a(x) = b(xN)] on one side and [a(x) = b(xM)] on the other side
    // xN and xM are replaced with the same lia var
    Set<UTerm> outVarTerms1 = collectRelatedSubTermSet(t1, outVar);
    Set<UTerm> outVarTerms2 = collectRelatedSubTermSet(t2, outVar);
    for (UTerm subt1 : outVarTerms1) {
      UName attr = getAttrRelatedToOutVar(subt1);
      if (attr == null) continue;
      UVar v1 = getVarRelatedToOutVar(subt1, attr);
      assert v1 != null;
      if (!v1.equals(commonTupleNewName)) continue; // only find uN in partner
      for (UTerm subt2 : outVarTerms2) {
        UVar v2 = getVarRelatedToOutVar(subt2, attr);
        if (v2 == null) continue;
        if (!bvs2.contains(v2)) continue; // only find xM in cur
        // found matching (uN,xM)
        return v2;
      }
    }

    // match var in subterm order
    UName tableName = UName.mk(commonTable);
    List<UVar> tableVars1 = collectTableVarList(t1, tableName);
    List<UVar> tableVars2 = collectTableVarList(t2, tableName);
    int bound = tableVars1.size();
    if (bound == tableVars2.size()) {
      for (int i = 0; i < bound; i++) {
        UVar v1 = tableVars1.get(i);
        UVar v2 = tableVars2.get(i);
        if (v1.equals(commonTupleNewName) && bvs2.contains(v2)) // only replace xM
        return v2;
      }
    }

    return null;
  }

  /**
   * Return the injected term, or null if the term is not injected. If "forceInjection" is true,
   * then the term must be injected. Besides, it also returns which bound var is replaced and
   * removed.
   */
  private UTerm injectTupleForOneUTerm(
      int curIndex,
      UVar commonTuple,
      int commonTupleAppearsIn,
      UVar commonTupleNewName,
      ArrayList<UVar> preVars,
      ArrayList<String> selectedVarTables,
      MatchingMode mode) {

    USum cur = (USum) sums.get(curIndex).copy();
    Set<UVar> boundTuples = cur.boundedVars();

    // directly replace the bound var in its source summation
    if (curIndex == commonTupleAppearsIn) {
      return replaceTargetTupleFromOneSum(cur, commonTuple, commonTupleNewName, preVars);
    }

    // get baseline for comparison among candidates
    Set<UTerm> diffBaseline = new HashSet<>();
    if (sumIndexSet1.contains(curIndex))
      diffBaseline.addAll(
          sumIndexSet2.stream()
              .filter(i -> previousSums.containsKey(i))
              .map(previousSums::get)
              .toList());
    if (sumIndexSet2.contains(curIndex))
      diffBaseline.addAll(
          sumIndexSet1.stream()
              .filter(i -> previousSums.containsKey(i))
              .map(previousSums::get)
              .toList());

    for (String commonTable : selectedVarTables) {
      Set<UVar> candidateVars = getCandidateVars(cur, commonTable);

      List<UVar> bests = null;
      int minDiff = Integer.MAX_VALUE;
      for (UVar tmp : candidateVars) {
        // evaluate a bound var
        // based on difference from previous sums
        USum curCopy = (USum) cur.copy();
        UTerm expAfterInject =
            replaceTargetTupleFromOneSum(curCopy, tmp, commonTupleNewName, new ArrayList<>());
        int diff = computeDiff(commonTupleNewName, expAfterInject, diffBaseline);

        // there is no best record, or "tmp" breaks the best record
        if (bests == null || diff < minDiff) {
          bests = new ArrayList<>();
          minDiff = diff;
        }
        // add "tmp" to bests if it is the best
        if (diff == minDiff) {
          bests.add(tmp);
        }
      }

      if (bests != null) {
        if (mode == MatchingMode.DIFF && bests.size() > 1 && minDiff < Integer.MAX_VALUE) {
          // Injection only uses DIFF,
          //   and there are multiple best candidates,
          //   and minDiff is not MAX_VALUE (i.e. baseline is not empty)
          // Delay matching the current summation
          return null;
        }
        // First, injection may use PATTERN;
        // Second, only one candidate is the best,
        //   or baseline is empty (so an arbitrary candidate can be selected)
        if (bests.size() == 1 || minDiff == Integer.MAX_VALUE) {
          return replaceTargetTupleFromOneSum(cur, bests.get(0), commonTupleNewName, preVars);
        }
        // inject using PATTERN among multiple bests
        UVar toReplace =
            chooseAmongMultiBests(curIndex, commonTable, commonTupleNewName, diffBaseline);
        if (toReplace != null)
          return replaceTargetTupleFromOneSum(cur, toReplace, commonTupleNewName, preVars);
      }
    }

    if (mode != MatchingMode.FORCE) return null;

    // force injection

    UVar v = null;
    int maxScore = 0;
    for (UVar tmp : boundTuples) {
      if (v == null) {
        v = tmp;
      } else {
        USum curCopy = (USum) cur.copy();
        UTerm expAfterInject =
            replaceTargetTupleFromOneSum(curCopy, v, commonTupleNewName, new ArrayList<>());
        int score = computeScoreForInjectTuple(previousSums.values(), expAfterInject);
        if (score > maxScore) v = tmp;
      }
    }
    selectedVarTables.add(findTableForTuple(cur.body(), v));
    return replaceTargetTupleFromOneSum(cur, v, commonTupleNewName, preVars);
  }

  private int computeScoreForInjectTuple(Collection<UTerm> sums, UTerm exp) {
    int score = 0;
    List<UTerm> thisTerms;
    if (exp instanceof USum sum) thisTerms = sum.body().subTerms();
    else if (exp instanceof UMul) thisTerms = exp.subTerms();
    else return 0;

    for (UTerm thatExp : sums) {
      int termScore = 0;

      List<UTerm> thatTerms;
      if (thatExp instanceof USum sum) thatTerms = sum.body().subTerms();
      else if (thatExp instanceof UMul) thatTerms = thatExp.subTerms();
      else thatTerms = new ArrayList<>();

      for (UTerm t : thatTerms) {
        if (thisTerms.contains(t)) termScore++;
      }

      if (termScore > score) score = termScore;
    }

    return score;
  }

  private <T> void setAll(List<T> l1, List<T> l2) {
    int i = 0;
    for (T t : l2) {
      l1.set(i++, t);
    }
  }

  /**
   * Decide upon a list of cases.
   *
   * <p>Upon each case, a decision is made by <code>tryToDecide</code>, and once a decision is made,
   * that case is consumed by <code>consumer</code> and removed from the list. This method keeps
   * applying <code>tryToDecide</code> to cases in the list until the list becomes empty or <code>
   * tryToDecide</code> returns <code>null</code> upon all the remaining cases in the list.
   *
   * <p>When <code>decideUndecided</code> is provided (i.e. non-null), <code>decideUndecided</code>
   * forces a decision upon each remaining case in the list after the procedure above.
   *
   * <p>This method finally returns whether each case has been given a decision.
   *
   * @param list the list of cases
   * @param tryToDecide the decision maker of each case
   * @param consumer the consumer that consumes each decided case and its decision
   * @param decideUndecided the decision maker that forces decisions upon undecided cases, or <code>
   *     null</code>
   * @return whether each case in <code>list</code> has been given a decision
   */
  private <T, U> boolean decide(
      List<T> list,
      Function<T, U> tryToDecide,
      BiConsumer<T, U> consumer,
      Function<T, U> decideUndecided) {
    List<T> toDecide = new LinkedList<>(), toDecideLater = list;

    // keep making decisions when there are cases that can be decided
    while (!toDecideLater.isEmpty() && !toDecide.equals(toDecideLater)) {
      // start a new round of decision
      toDecide = toDecideLater;
      toDecideLater = new LinkedList<>();
      // traverse the remaining cases (in "toDecide")
      for (T t : toDecide) {
        // try to decide upon each case
        U result = tryToDecide.apply(t);
        if (result == null) {
          // the case cannot be decided in the current round
          // delay its decision
          toDecideLater.add(t);
        } else {
          consumer.accept(t, result);
        }
      }
    }

    toDecide = toDecideLater;

    // when decisions are not forced
    // return whether decisions are made successfully upon all cases
    if (decideUndecided == null) return toDecide.isEmpty();

    if (toDecide.isEmpty()) return true;

    // force decision upon undecided cases
    T toForce = toDecide.get(0);
    U result = decideUndecided.apply(toForce);
    assert result != null;
    consumer.accept(toForce, result);
    // retry the decision procedure over the remaining cases
    return decide(toDecide.subList(1, toDecide.size()), tryToDecide, consumer, decideUndecided);
  }

  /**
   * Given a selected table and tuple, try to inject. Return the replaced bound vars, or null on
   * failure.
   */
  private Set<UVar> tryInjectTuple(
      CommonTupleResult common, UVar commonTupleNewName, MatchingMode mode) {
    // reset
    previousSums = new HashMap<>();
    List<UTerm> injectedSums = new ArrayList<>(sums);

    UVar commonTuple = common.tuple;
    String commonTable = common.table;
    int commonAppearsIn = common.appearsIn;

    ArrayList<UVar> selectedVars = new ArrayList<>();
    ArrayList<String> selectedVarTables = new ArrayList<>();
    selectedVarTables.add(commonTable);

    List<Integer> toMatch = new LinkedList<>();
    toMatch.add(commonAppearsIn);
    for (int i = 0; i < sums.size(); i++) if (i != commonAppearsIn) toMatch.add(i);

    boolean result =
        decide(
            toMatch,
            i ->
                injectTupleForOneUTerm(
                    i,
                    commonTuple,
                    commonAppearsIn,
                    commonTupleNewName,
                    selectedVars,
                    selectedVarTables,
                    mode == MatchingMode.FORCE ? MatchingMode.PATTERN : mode),
            (i, injectedTerm) -> {
              injectedSums.set(i, injectedTerm);
              previousSums.put(i, injectedTerm);
            },
            mode == MatchingMode.FORCE
                ? i ->
                    injectTupleForOneUTerm(
                        i,
                        commonTuple,
                        commonAppearsIn,
                        commonTupleNewName,
                        selectedVars,
                        selectedVarTables,
                        mode)
                : null);

    // upon success, update the sum list in-place
    if (result) {
      setAll(sums, injectedSums);
      return new HashSet<>(selectedVars);
    }
    return null;
  }

  /**
   * Match bound variables in summations, where summations are divided into two sets according to
   * which query they belong to.
   *
   * @param sums the summations
   * @param sumIndexSet1 the first set of indices, each of which refers to a summation in "sums"
   * @param sumIndexSet2 the second set of indices similar to "sumIndexSet1"
   * @return the result of injection
   */
  public Result injectCommonTuple(
      List<UTerm> sums, Set<Integer> sumIndexSet1, Set<Integer> sumIndexSet2) {
    if (sums.isEmpty()) return null;

    this.sums = sums;
    this.sumIndexSet1 = sumIndexSet1;
    this.sumIndexSet2 = sumIndexSet2;

    UVar commonTupleNewName = UVar.mkBase(UName.mk(boundVarName.next()));
    List<CommonTupleResult> commons = new LinkedList<>(getCommonTuples());
    assert !commons.isEmpty();

    // only use DIFF
    for (CommonTupleResult common : commons) {
      // traverse possible common tuples until success or end
      // try a different common tuple name if matching is stuck
      Set<UVar> replacedBoundVars = tryInjectTuple(common, commonTupleNewName, MatchingMode.DIFF);
      if (replacedBoundVars != null) return new Result(commonTupleNewName, replacedBoundVars);
    }

    // use DIFF first, then PATTERN
    for (CommonTupleResult common : commons) {
      // traverse possible common tuples until success or end
      // try a different common tuple name if matching is stuck
      Set<UVar> replacedBoundVars =
          tryInjectTuple(common, commonTupleNewName, MatchingMode.PATTERN);
      if (replacedBoundVars != null) return new Result(commonTupleNewName, replacedBoundVars);
    }

    // all possible common tuples lead to stuck in matching
    // (i.e. neither DIFF nor PATTERN works)
    // force injection on an arbitrary common tuple
    Set<UVar> replacedBoundVars =
        tryInjectTuple(commons.get(0), commonTupleNewName, MatchingMode.FORCE);
    assert replacedBoundVars != null;
    return new Result(commonTupleNewName, replacedBoundVars);
  }

  /**
   * Create a matcher to match bound variables in summations.
   *
   * @param boundVarName where new bound var names come from
   * @param outVar output var name (typically "x")
   */
  public HeuristicBoundVarMatcher(NameSequence boundVarName, UVar outVar) {
    this.boundVarName = boundVarName;
    this.outVar = outVar;
  }
}
