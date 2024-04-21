package sqlsolver.superopt.liastar.transformer;

import static sqlsolver.superopt.util.VectorSupport.*;
import static sqlsolver.superopt.util.Z3Support.defineNonNegativeVars;

import com.microsoft.z3.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import sqlsolver.superopt.liastar.LiaStar;

public class SemiLinearSet {
  private static long globalId = 0L;

  private final long id;
  private final List<LinearSet> linearSets;

  public static synchronized long newId() {
    return globalId++;
  }

  public SemiLinearSet() {
    linearSets = new ArrayList<>();
    id = newId();
  }

  public int size() {
    return linearSets.size();
  }

  public void add(LinearSet ls) {
    linearSets.add(ls);
  }

  public void remove(LinearSet ls) {
    linearSets.remove(ls);
  }

  public LinearSet get(int index) {
    return linearSets.get(index);
  }

  /**
   * Construct a Z3 formula: "vector is not in this SLS". That is, vector is not in any of the
   * linear sets in this SLS.
   */
  public BoolExpr toLiaZ3Neg(Context ctx, Map<String, Expr> varDef, List<String> vector) {
    if (linearSets.isEmpty()) {
      return ctx.mkTrue();
    }
    // var definitions & non-negativity: lambda >= 0
    // TODO: this implementation requires that
    //  the outer LIA formula does not contain var "ls_lambda_..."
    final int lambdaSize =
        linearSets.stream().map(LinearSet::offsetSize).max(Integer::compareTo).get();
    final List<String> lambda = createVarVector("ls_lambda", 0, lambdaSize);
    final BoolExpr nn = defineNonNegativeVars(ctx, varDef, lambda);
    // body: vector not in LS1 /\ ...
    BoolExpr body = null;
    for (LinearSet ls : linearSets) {
      final BoolExpr lsZ3 =
          ls.toLiaZ3NegQf(ctx, varDef, vector, lambda.subList(0, ls.offsetSize()));
      body = body == null ? lsZ3 : ctx.mkAnd(body, lsZ3);
    }
    assert body != null;
    // forall lambda. lambda >= 0 -> vector not in LS1 /\ ...
    if (lambdaSize == 0) {
      return body;
    }
    return ctx.mkForall(
        lambda.stream().map(varDef::get).toList().toArray(new Expr[0]),
        ctx.mkImplies(nn, body),
        0,
        null,
        null,
        null,
        null);
  }

  /**
   * Construct a Z3 formula: "vector is in the additive closure of this SLS". That is, "there exists
   * non-negative mu and lambda such that vector = \sum{i=1 to k}(mu_i * shift_i + lambda_i *
   * offsets_i) and mu_i = 0 implies lambda_i = 0", where k is the number of linear sets, and
   * shift_i/offsets_i are the shift and offset vectors of the i-th linear set.
   */
  // TODO: the outer LIA formula should not contain var "sls_lambda_..." or "sls_mu_..."
  public LiaStar toStarLia(List<String> vector) {
    final int lsCount = linearSets.size();
    // empty SLS only allows vector = 0
    if (lsCount == 0) {
      return eq(nameToLia(vector), liaZero(vector.size()));
    }
    // construct non-negativity constraints
    final String slsPrefix = "sls_" + id + "_";
    final List<String> mu = createVarVector(slsPrefix + "mu", 0, lsCount);
    final List<List<String>> lambda = new ArrayList<>();
    for (int i = 0; i < lsCount; i++) {
      lambda.add(createVarVector(slsPrefix + "lambda_" + i, 0, linearSets.get(i).offsetSize()));
    }
    LiaStar nnConstr = nonNegative(nameToLia(mu));
    for (List<String> lambdaI : lambda) {
      nnConstr = LiaStar.mkAnd(false, nnConstr, nonNegative(nameToLia(lambdaI)));
    }
    // construct "vector = \sum..."
    List<LiaStar> sum = null;
    for (int i = 0; i < lsCount; i++) {
      final LinearSet ls = linearSets.get(i);
      final List<Long> shift = ls.getShift();
      final List<List<Long>> offsets = ls.getOffsets();
      final List<LiaStar> itemShift = times(constToLia(shift), LiaStar.mkVar(false, mu.get(i)));
      final List<LiaStar> itemOffsets =
          linearCombination(nameToLia(lambda.get(i)), offsets, shift.size());
      final List<LiaStar> term = plus(itemShift, itemOffsets);
      sum = sum == null ? term : plus(sum, term);
    }
    final LiaStar sumConstr = eq(nameToLia(vector), sum);
    // construct "mu_i = 0 -> lambda_i = 0"
    LiaStar impConstr = null;
    for (int i = 0; i < lsCount; i++) {
      final LiaStar muIisZero =
          LiaStar.mkEq(false, LiaStar.mkVar(false, mu.get(i)), LiaStar.mkConst(false, 0));
      final LiaStar lambdaIisZero =
          eq(nameToLia(lambda.get(i)), liaZero(linearSets.get(i).offsetSize()));
      final LiaStar impI = LiaStar.mkImplies(false, muIisZero, lambdaIisZero);
      impConstr = impConstr == null ? impI : LiaStar.mkAnd(false, impConstr, impI);
    }
    // return
    return LiaStar.mkAndNoStar(nnConstr, sumConstr, impConstr);
  }

  @Override
  public String toString() {
    return linearSets.toString();
  }
}
