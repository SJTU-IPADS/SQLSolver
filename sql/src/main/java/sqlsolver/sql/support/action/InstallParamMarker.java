package sqlsolver.sql.support.action;

import sqlsolver.sql.ast.ExprFields;
import sqlsolver.sql.ast.ExprKind;
import sqlsolver.sql.ast.SqlContext;
import sqlsolver.sql.support.resolution.ParamDesc;
import sqlsolver.sql.support.resolution.Params;
import sqlsolver.sql.ast.SqlNode;

class InstallParamMarker {
  public static void normalize(SqlNode root) {
    final Params params = root.context().getAdditionalInfo(Params.PARAMS);
    params.forEach(InstallParamMarker::installParamMarker);
  }

  private static void installParamMarker(ParamDesc param) {
    final SqlContext context = param.node().context();
    final SqlNode paramMarker = SqlNode.mk(context, ExprKind.Param);
    paramMarker.$(ExprFields.Param_Number, param.index());
    context.displaceNode(param.node().nodeId(), paramMarker.nodeId());
  }
}
