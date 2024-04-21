package sqlsolver.superopt.liastar.transformer;

import static sqlsolver.superopt.util.VectorSupport.*;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import java.util.List;
import java.util.Map;
import sqlsolver.superopt.liastar.LiaStar;
import sqlsolver.superopt.util.VectorSupport;

public class LinearSet {
  private final List<Long> shift;
  private final List<List<Long>> offsets;

  LinearSet(List<Long> shift, List<List<Long>> offsets) {
    this.shift = shift;
    // zero offsets are useless
    this.offsets = offsets.stream().filter(v -> !isConstZero(v)).toList();
  }

  /** Get the shift vector of this LS. Note that the vector is copied. */
  public List<Long> getShift() {
    return VectorSupport.constCopy(shift);
  }

  /** Get the offset vectors of this LS. Note that the vectors are copied. */
  public List<List<Long>> getOffsets() {
    return offsets.stream().map(VectorSupport::constCopy).toList();
  }

  public int offsetSize() {
    return offsets.size();
  }

  /**
   * Construct a Z3 formula: "vector is not in this LS". That is, for all non-negative lambda,
   * "vector != shift + lambda * offsets", where "lambda * offsets" means a linear combination of
   * offsets and lambda is the vector of coefficients. Note that quantifiers and non-negativity are
   * provided outside.
   */
  public BoolExpr toLiaZ3NegQf(
          Context ctx, Map<String, Expr> varDef, List<String> vector, List<String> lambda) {
    if (offsets.isEmpty()) {
      // vector != shift
      return ctx.mkNot(
          (BoolExpr) (eq(nameToLia(vector), constToLia(shift))).transToSMT(ctx, varDef));
    }
    // vector != shift + lambda * offsets
    final List<LiaStar> leftLia = nameToLia(vector);
    final List<LiaStar> rightLia =
        plus(constToLia(shift), linearCombination(nameToLia(lambda), offsets, shift.size()));
    final LiaStar lia = LiaStar.mkNot(false, eq(leftLia, rightLia));
    return (BoolExpr) lia.transToSMT(ctx, varDef);
  }

  @Override
  public String toString() {
    return "LS(shift=" + shift + ";offsets=" + offsets + ")";
  }
}
