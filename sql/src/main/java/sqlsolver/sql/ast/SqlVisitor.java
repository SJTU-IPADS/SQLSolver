package sqlsolver.sql.ast;

import sqlsolver.common.field.FieldKey;

import java.util.function.Consumer;

public interface SqlVisitor {
  default boolean enter(SqlNode node) {
    return true;
  }

  default void leave(SqlNode node) {}

  default boolean enterChild(SqlNode parent, FieldKey<SqlNode> key, SqlNode child) {
    return true;
  }

  default void leaveChild(SqlNode parent, FieldKey<SqlNode> key, SqlNode child) {}

  default boolean enterChildren(SqlNode parent, FieldKey<SqlNodes> key, SqlNodes child) {
    return true;
  }

  default void leaveChildren(SqlNode parent, FieldKey<SqlNodes> key, SqlNodes child) {}

  default boolean enterCreateTable(SqlNode createTable) {
    return true;
  }

  default void leaveCreateTable(SqlNode createTable) {}

  default boolean enterName2(SqlNode name2) {
    return true;
  }

  default void leaveName2(SqlNode name2) {}

  default boolean enterName3(SqlNode name3) {
    return true;
  }

  default void leaveName3(SqlNode name3) {}

  default boolean enterTableName(SqlNode tableName) {
    return true;
  }

  default void leaveTableName(SqlNode tableName) {}

  default boolean enterColumnDef(SqlNode colDef) {
    return true;
  }

  default void leaveColumnDef(SqlNode colDef) {}

  default boolean enterReferences(SqlNode ref) {
    return true;
  }

  default void leaveReferences(SqlNode ref) {}

  default boolean enterColumnName(SqlNode colName) {
    return true;
  }

  default void leaveColumnName(SqlNode colName) {}

  default boolean enterIndexDef(SqlNode indexDef) {
    return true;
  }

  default void leaveIndexDef(SqlNode indexDef) {}

  default boolean enterKeyPart(SqlNode keyPart) {
    return true;
  }

  default void leaveKeyPart(SqlNode keyPart) {}

  default boolean enterVariable(SqlNode variable) {
    return true;
  }

  default void leaveVariable(SqlNode variable) {}

  default boolean enterColumnRef(SqlNode columnRef) {
    return true;
  }

  default void leaveColumnRef(SqlNode columnRef) {}

  default boolean enterLiteral(SqlNode literal) {
    return true;
  }

  default void leaveLiteral(SqlNode literal) {}

  default boolean enterFuncCall(SqlNode funcCall) {
    return true;
  }

  default void leaveFuncCall(SqlNode funcCall) {}

  default boolean enterCollation(SqlNode collation) {
    return true;
  }

  default void leaveCollation(SqlNode collation) {}

  default boolean enterParamMarker(SqlNode paramMarker) {
    return true;
  }

  default void leaveParamMarker(SqlNode paramMarker) {}

  default boolean enterUnary(SqlNode unary) {
    return true;
  }

  default void leaveUnary(SqlNode unary) {}

  default boolean enterGroupingOp(SqlNode groupingOp) {
    return true;
  }

  default void leaveGroupingOp(SqlNode groupingOp) {}

  default boolean enterTuple(SqlNode tuple) {
    return true;
  }

  default void leaveTuple(SqlNode tuple) {}

  default boolean enterMatch(SqlNode match) {
    return true;
  }

  default void leaveMatch(SqlNode match) {}

  default boolean enterCast(SqlNode cast) {
    return true;
  }

  default void leaveCast(SqlNode cast) {}

  default boolean enterSymbol(SqlNode symbol) {
    return true;
  }

  default void leaveSymbol(SqlNode symbol) {}

  default boolean enterDefault(SqlNode _default) {
    return true;
  }

  default void leaveDefault(SqlNode _default) {}

  default boolean enterValues(SqlNode values) {
    return true;
  }

  default void leaveValues(SqlNode values) {}

  default boolean enterInterval(SqlNode interval) {
    return true;
  }

  default void leaveInterval(SqlNode interval) {}

  default boolean enterExists(SqlNode exists) {
    return true;
  }

  default void leaveExists(SqlNode exists) {}

  default boolean enterQueryExpr(SqlNode queryExpr) {
    return true;
  }

  default void leaveQueryExpr(SqlNode queryExpr) {}

  default boolean enterWildcard(SqlNode wildcard) {
    return true;
  }

  default void leaveWildcard(SqlNode wildcard) {}

  default boolean enterAggregate(SqlNode aggregate) {
    return true;
  }

  default void leaveAggregate(SqlNode aggregate) {}

  default boolean enterConvertUsing(SqlNode convertUsing) {
    return true;
  }

  default void leaveConvertUsing(SqlNode convertUsing) {}

  default boolean enterCase(SqlNode _case) {
    return true;
  }

  default void leaveCase(SqlNode _case) {}

  default boolean enterWhen(SqlNode when) {
    return true;
  }

  default void leaveWhen(SqlNode when) {}

  default boolean enterBinary(SqlNode binary) {
    return true;
  }

  default void leaveBinary(SqlNode binary) {}

  default boolean enterFrameBound(SqlNode frameBound) {
    return true;
  }

  default void leaveFrameBound(SqlNode frameBound) {}

  default boolean enterWindowFrame(SqlNode windowFrame) {
    return true;
  }

  default void leaveWindowFrame(SqlNode windowFrame) {}

  default boolean enterWindowSpec(SqlNode windowSpec) {
    return true;
  }

  default void leaveWindowSpec(SqlNode windowSpec) {}

  default boolean enterOrderItem(SqlNode orderItem) {
    return true;
  }

  default void leaveOrderItem(SqlNode orderItem) {}

  default boolean enterTernary(SqlNode ternary) {
    return true;
  }

  default void leaveTernary(SqlNode ternary) {}

  default boolean enterSelectItem(SqlNode selectItem) {
    return true;
  }

  default void leaveSelectItem(SqlNode selectItem) {}

  default boolean enterIndexHint(SqlNode indexHint) {
    return true;
  }

  default void leaveIndexHint(SqlNode indexHint) {}

  default boolean enterSimpleTableSource(SqlNode simpleTableSource) {
    return true;
  }

  default void leaveSimpleTableSource(SqlNode simpleTableSource) {}

  default boolean enterDerivedTableSource(SqlNode derivedTableSource) {
    return true;
  }

  default void leaveDerivedTableSource(SqlNode derivedTableSource) {}

  default boolean enterJoinedTableSource(SqlNode joinedTableSource) {
    return true;
  }

  default void leaveJoinedTableSource(SqlNode joinedTableSource) {}

  default boolean enterQuery(SqlNode query) {
    return true;
  }

  default void leaveQuery(SqlNode query) {}

  default boolean enterQuerySpec(SqlNode querySpec) {
    return true;
  }

  default void leaveQuerySpec(SqlNode querySpec) {}

  default boolean enterSetOp(SqlNode union) {
    return true;
  }

  default void leaveSetOp(SqlNode union) {}

  default boolean enterStatement(SqlNode statement) {
    return true;
  }

  default void leaveStatement(SqlNode statement) {}

  default boolean enterIndirection(SqlNode indirection) {
    return true;
  }

  default void leaveIndirection(SqlNode indirection) {}

  default boolean enterIndirectionComp(SqlNode indirectionComp) {
    return true;
  }

  default void leaveIndirectionComp(SqlNode indirectionComp) {}

  default boolean enterCommonName(SqlNode commonName) {
    return true;
  }

  default void leaveCommonName(SqlNode commonName) {}

  default boolean enterArray(SqlNode array) {
    return true;
  }

  default void leaveArray(SqlNode array) {}

  default boolean enterGroupItem(SqlNode groupItem) {
    return true;
  }

  default void leaveGroupItem(SqlNode groupItem) {}

  static SqlVisitor topDownVisit(Consumer<SqlNode> func) {
    return new SqlVisitor() {
      @Override
      public boolean enter(SqlNode node) {
        func.accept(node);
        return true;
      }
    };
  }

  static SqlVisitor topDownVisit(Consumer<SqlNode> func, FieldDomain... types) {
    return new SqlVisitor() {
      @Override
      public boolean enter(SqlNode node) {
        for (FieldDomain type : types)
          if (type.isInstance(node)) {
            func.accept(node);
            break;
          }
        return true;
      }
    };
  }
}
