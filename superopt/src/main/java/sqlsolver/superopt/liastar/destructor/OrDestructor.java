package sqlsolver.superopt.liastar.destructor;

import sqlsolver.superopt.liastar.LiaOrImpl;
import sqlsolver.superopt.liastar.LiaStar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static sqlsolver.superopt.liastar.LiaStar.*;

public class OrDestructor extends Destructor {
  public OrDestructor(Set<String> params, Set<String> importantVars) {
    super(params, importantVars);
  }

  @Override
  public List<LiaStar> destruct(LiaStar formula) {
    // destruct a term f if f has params and is not separate from non-param part
    final List<LiaStar> noParamPart = new ArrayList<>();
    final List<LiaStar> paramPart = new ArrayList<>();
    classifyConjunctionLiteralsByParam(formula, noParamPart, paramPart);
    // decide which terms should be destructed
    final List<List<LiaStar>> toDestruct = new ArrayList<>();
    final Set<String> noParamPartVars = collectVarNames(noParamPart);
    final List<LiaStar> undestructed = new ArrayList<>(noParamPart);
    for (LiaStar paramTerm : paramPart) {
      if (paramTerm instanceof LiaOrImpl or &&
              !isSeparateFrom(collectVarNames(paramPart), noParamPartVars)) {
        // destruct terms that prevent separation
        toDestruct.add(decomposeDNF(or));
      } else {
        undestructed.add(paramTerm);
      }
    }
    // destruct terms
    final LiaStar resultBase = mkConjunction(true, undestructed);
    final List<LiaStar> results = new ArrayList<>();
    destruct(results, toDestruct, 0, resultBase);
    return results;
  }

  private void destruct(List<LiaStar> results, List<List<LiaStar>> toDestruct, int depth, LiaStar result) {
    if (depth >= toDestruct.size()) {
      results.add(result);
      return;
    }
    for (LiaStar cas : toDestruct.get(depth)) {
      destruct(results, toDestruct, depth + 1, mkAnd(true, result.deepcopy(), cas));
    }
  }
}
