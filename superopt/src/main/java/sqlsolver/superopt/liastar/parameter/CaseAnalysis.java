package sqlsolver.superopt.liastar.parameter;

import sqlsolver.superopt.liastar.LiaNotImpl;
import sqlsolver.superopt.liastar.LiaStar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static sqlsolver.common.utils.ListSupport.*;

/**
 * Do case analysis with LIA* formulas.
 */
public class CaseAnalysis {
  private final List<SemanticLiaStar> caseUnits;

  public CaseAnalysis() {
    caseUnits = new ArrayList<>();
  }

  public int getUnitCount() {
    return caseUnits.size();
  }

  /**
   * Add a case unit: literal 1 /\ ... /\ literal N.
   */
  public void addCaseUnit(Collection<LiaStar> literals) {
    for (LiaStar lit : literals) {
      if (lit instanceof LiaNotImpl not) {
        lit = not.subNodes().get(0);
      }
      final SemanticLiaStar semLit = new SemanticLiaStar(lit);
      final SemanticLiaStar semLitNeg = new SemanticLiaStar(LiaStar.mkNot(false, lit));
      if (!caseUnits.contains(semLit) && !caseUnits.contains(semLitNeg)) {
        caseUnits.add(semLit);
      }
    }
    // TODO: (enhanced impl)
    //  If the unit A intersects with another unit B,
    //  they break down into smaller units: A-B, B-A, A*B (intersection).
  }

  /**
   * Return a list of cases that form the whole space.
   * Specifically, return<br/>
   * [caseUnit1 /\ caseUnit2 /\ ... /\ caseUnitN,<br/>
   * caseUnit1 /\ caseUnit2 /\ ... /\ ~caseUnitN,<br/>
   * ...<br/>
   * caseUnit1 /\ ~caseUnit2 /\ ... /\ caseUnitN,<br/>
   * caseUnit1 /\ ~caseUnit2 /\ ... /\ ~caseUnitN,<br/>
   * ...<br/>
   * ~caseUnit1 /\ caseUnit2 /\ ... /\ caseUnitN,<br/>
   * ~caseUnit1 /\ caseUnit2 /\ ... /\ ~caseUnitN,<br/>
   * ...<br/>
   * ~caseUnit1 /\ ~caseUnit2 /\ ... /\ caseUnitN,<br/>
   * ~caseUnit1 /\ ~caseUnit2 /\ ... /\ ~caseUnitN,<br/>
   * ...].
   */
  public List<LiaStar> destruct() {
    final List<LiaStar> result = new ArrayList<>();
    destructRecursive(result, new ArrayList<>(), 0);
    return result;
  }

  private void destructRecursive(List<LiaStar> cases, List<LiaStar> cas, int depth) {
    if (depth == caseUnits.size()) {
      final List<LiaStar> casCopy = map(cas, f -> f.deepcopy());
      cases.add(LiaStar.mkConjunction(false, casCopy));
      return;
    }
    final LiaStar unit = caseUnits.get(depth).formula();
    final LiaStar unitNeg = LiaStar.mkNot(false, unit);
    cas.add(unit);
    destructRecursive(cases, cas, depth + 1);
    cas.remove(depth);
    cas.add(unitNeg);
    destructRecursive(cases, cas, depth + 1);
    cas.remove(depth);
  }
}
