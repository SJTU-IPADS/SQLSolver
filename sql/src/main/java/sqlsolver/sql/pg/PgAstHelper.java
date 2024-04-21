package sqlsolver.sql.pg;

import org.antlr.v4.runtime.tree.TerminalNode;
import sqlsolver.common.utils.ListSupport;
import sqlsolver.sql.pg.internal.PGParser;
import sqlsolver.sql.ast.SqlDataType;
import sqlsolver.sql.ast.constants.Category;
import sqlsolver.sql.ast.constants.DataTypeName;
import sqlsolver.sql.ast.constants.IndexKind;
import sqlsolver.sql.ast.constants.JoinKind;

import java.util.Set;
import java.util.function.Function;

import static sqlsolver.common.utils.Commons.assertFalse;
import static sqlsolver.common.utils.Commons.unquoted;
import static sqlsolver.sql.ast.constants.IndexKind.*;

public interface PgAstHelper {
  static String stringifyIdentifier(PGParser.Id_tokenContext ctx) {
    if (ctx == null) return null;
    // the parser rule has quoted the text already
    return ctx.getText();
  }

  static String stringifyIdentifier(PGParser.Identifier_nontypeContext ctx) {
    if (ctx == null) return null;
    if (ctx.tokens_nonreserved() != null || ctx.tokens_reserved_except_function_type() != null)
      return ctx.getText();
    return stringifyIdentifier(ctx.id_token());
  }

  static String stringifyIdentifier(PGParser.IdentifierContext ctx) {
    if (ctx == null) return null;
    if (ctx.tokens_nonreserved() != null || ctx.tokens_nonreserved_except_function_type() != null)
      return ctx.getText();
    return stringifyIdentifier(ctx.id_token());
  }

  static String stringifyIdentifier(PGParser.Col_labelContext ctx) {
    if (ctx == null) return null;
    if (ctx.id_token() != null) return stringifyIdentifier(ctx.id_token());
    else return ctx.getText();
  }

  /** @return String[3] */
  static String[] stringifyIdentifier(PGParser.Schema_qualified_nameContext ctx) {
    if (ctx == null) return null;
    final var identifiers = ctx.identifier();
    final String str0, str1, str2;
    if (identifiers.size() == 3) {
      str0 = stringifyIdentifier(identifiers.get(0));
      str1 = stringifyIdentifier(identifiers.get(1));
      str2 = stringifyIdentifier(identifiers.get(2));

    } else if (identifiers.size() == 2) {
      str0 = null;
      str1 = stringifyIdentifier(identifiers.get(0));
      str2 = stringifyIdentifier(identifiers.get(1));

    } else if (identifiers.size() == 1) {
      str0 = null;
      str1 = null;
      str2 = stringifyIdentifier(identifiers.get(0));

    } else return assertFalse();

    final String[] triple = new String[3];
    triple[0] = str0;
    triple[1] = str1;
    triple[2] = str2;

    return triple;
  }

  static String[] stringifyIdentifier(PGParser.Schema_qualified_name_nontypeContext ctx) {
    if (ctx == null) return null;
    final String[] pair = new String[2];
    if (ctx.schema != null) pair[0] = stringifyIdentifier(ctx.schema);
    pair[1] = stringifyIdentifier(ctx.identifier_nontype());
    return pair;
  }

  static String stringifyText(PGParser.Character_stringContext ctx) {
    if (ctx == null) return null;
    if (ctx.Text_between_Dollar() != null && !ctx.Text_between_Dollar().isEmpty())
      return String.join("", ListSupport.<TerminalNode, String>map((Iterable<TerminalNode>) ctx.Text_between_Dollar(), (Function<? super TerminalNode, ? extends String>) TerminalNode::getText));
    else if (ctx.Character_String_Literal() != null) return unquoted(ctx.getText(), '\'');
    else return assertFalse();
  }

  static IndexKind parseIndexKind(String text) {
    if (text == null) return null;
    return switch (text) {
      case "btree" -> BTREE;
      case "hash" -> HASH;
      case "rtree", "gist" -> GIST;
      case "gin" -> GIN;
      case "spgist" -> SPGIST;
      case "brin" -> BRIN;
      default -> null;
    };
  }

  static SqlDataType parseDataType(PGParser.Data_typeContext ctx) {
    final var predefinedCtx = ctx.predefined_type();
    final String typeString =
        predefinedCtx.type != null
            ? predefinedCtx.type.getText().toLowerCase()
            : stringifyIdentifier(
            predefinedCtx.schema_qualified_name_nontype().identifier_nontype())
            .toLowerCase();

    final String name;
    final Category category;
    if (typeString.endsWith("int") || typeString.equals(DataTypeName.INTEGER)) {
      category = Category.INTEGRAL;
      name = typeString.equals("int") ? DataTypeName.INTEGER : typeString;

    } else if (typeString.contains("bit")) {
      category = Category.BIT_STRING;
      name =
          (predefinedCtx.VARYING() != null || typeString.contains("var"))
              ? DataTypeName.BIT_VARYING
              : DataTypeName.BIT;

    } else if (typeString.startsWith("int") && !typeString.equals("interval")) {
      category = Category.INTEGRAL;
      switch (typeString) {
        case "int2":
          name = DataTypeName.SMALLINT;
          break;
        case "int4":
          name = DataTypeName.INTEGER;
          break;
        case "int8":
          name = DataTypeName.BIGINT;
          break;
        default:
          return assertFalse();
      }

    } else if (typeString.endsWith("serial")) {
      category = Category.INTEGRAL;
      name = typeString;

    } else if (typeString.startsWith("serial")) {
      category = Category.INTEGRAL;
      switch (typeString) {
        case "serial2":
          name = DataTypeName.SMALLSERIAL;
          break;
        case "serial4":
          name = DataTypeName.SERIAL;
          break;
        case "serial8":
          name = DataTypeName.BIGSERIAL;
          break;
        default:
          return assertFalse();
      }

    } else if (DataTypeName.FRACTION_TYPES.contains(typeString) || "dec".equals(typeString)) {
      category = Category.FRACTION;
      name = "dec".equals(typeString) ? DataTypeName.DECIMAL : typeString;

    } else if (typeString.startsWith("float")) {
      category = Category.FRACTION;
      switch (typeString) {
        case "float4":
          name = DataTypeName.FLOAT;
          break;
        case "float8":
          name = DataTypeName.DOUBLE;
          break;
        default:
          return assertFalse();
      }

    } else if (typeString.equals("text")) {
      category = Category.STRING;
      name = typeString;

    } else if (DataTypeName.TIME_TYPE.contains(typeString)) {
      category = Category.TIME;
      name = predefinedCtx.WITH() != null ? (typeString + "tz") : typeString;

    } else if (typeString.contains("char")) {
      category = Category.STRING;
      name =
          (typeString.contains("var") || predefinedCtx.VARYING() != null)
              ? DataTypeName.VARCHAR
              : DataTypeName.CHAR;

    } else if (typeString.equals("interval")) {
      category = Category.INTERVAL;
      name = DataTypeName.INTERVAL;

    } else if (typeString.contains("bool")) {
      category = Category.BOOLEAN;
      name = DataTypeName.BOOLEAN;

    } else if (typeString.contains("json")) {
      category = Category.JSON;
      name = typeString;

    } else if (DataTypeName.GEOMETRY_TYPES.contains(typeString)) {
      category = Category.GEO;
      name = typeString;

    } else if (DataTypeName.NET_TYPES.contains(typeString)) {
      category = Category.NET;
      name = typeString;

    } else if (DataTypeName.MONEY.equals(typeString)) {
      category = Category.MONETARY;
      name = typeString;

    } else if (DataTypeName.UUID.equals(typeString)) {
      category = Category.UUID;
      name = typeString;

    } else if (DataTypeName.XML.equals(typeString)) {
      category = Category.XML;
      name = typeString;

    } else if (typeString.endsWith("range")) {
      category = Category.RANGE;
      name = typeString;

    } else {
      category = Category.UNCLASSIFIED;
      name = typeString;
    }

    final var typeLength = predefinedCtx.type_length();
    final var precision = predefinedCtx.precision_param();
    final var intervalField = predefinedCtx.interval_field();

    final int w, p;
    if (typeLength != null) {
      w = Integer.parseInt(typeLength.NUMBER_LITERAL().getText());
      p = -1;

    } else if (precision != null) {
      w = Integer.parseInt(precision.precision.getText());
      p = precision.scale == null ? -1 : Integer.parseInt(precision.scale.getText());

    } else {
      w = -1;
      p = -1;
    }

    final String interval = intervalField == null ? null : intervalField.getText();

    final var arrayDimsCtx = ctx.array_type();
    final int[] arrayDims = new int[arrayDimsCtx.size()];
    for (int i = 0; i < arrayDimsCtx.size(); i++) {
      final var arrayDimCtx = arrayDimsCtx.get(i);
      arrayDims[i] =
          arrayDimCtx.NUMBER_LITERAL() == null
              ? 0
              : Integer.parseInt(arrayDimCtx.NUMBER_LITERAL().getText());
    }

    return SqlDataType.mk(category, name, w, p)
                      .setIntervalField(interval)
                      .setDimensions(arrayDims);
  }

  static Number parseNumericLiteral(PGParser.Unsigned_numeric_literalContext ctx) {
    if (ctx.REAL_NUMBER() != null) return Double.parseDouble(ctx.getText());
    else if (ctx.NUMBER_LITERAL() != null) return Long.parseLong(ctx.getText());
    else return assertFalse();
  }

  static Boolean parseTruthValue(PGParser.Truth_valueContext ctx) {
    if (ctx.TRUE() != null || ctx.ON() != null) return true;
    else if (ctx.FALSE() != null) return false;
    else return assertFalse();
  }

  static Object parseUnsignedValue(PGParser.Unsigned_value_specificationContext ctx) {
    if (ctx.unsigned_numeric_literal() != null)
      return parseNumericLiteral(ctx.unsigned_numeric_literal());
    else if (ctx.character_string() != null) return stringifyText(ctx.character_string());
    else if (ctx.truth_value() != null) return parseTruthValue(ctx.truth_value());
    else return assertFalse();
  }

  static String parseAlias(PGParser.Alias_clauseContext ctx) {
    return stringifyIdentifier(ctx.alias);
  }

  static JoinKind parseJoinKind(PGParser.From_itemContext ctx) {
    if (ctx == null) return null;
    if (ctx.NATURAL() != null) {
      if (ctx.LEFT() != null) return JoinKind.NATURAL_LEFT_JOIN;
      else if (ctx.RIGHT() != null) return JoinKind.NATURAL_RIGHT_JOIN;
      else return JoinKind.NATURAL_INNER_JOIN;
    }

    if (ctx.CROSS() != null) return JoinKind.CROSS_JOIN;
    if (ctx.INNER() != null) return JoinKind.INNER_JOIN;
    if (ctx.LEFT() != null) return JoinKind.LEFT_JOIN;
    if (ctx.RIGHT() != null) return JoinKind.RIGHT_JOIN;
    if (ctx.FULL() != null) return JoinKind.FULL_JOIN;

    return JoinKind.INNER_JOIN;
  }

  static int typeLength2Int(PGParser.Type_lengthContext ctx) {
    return Integer.parseInt(ctx.NUMBER_LITERAL().getText());
  }

  Set<String> KNOWN_AGG_BASIC =
      Set.of(
          "array_agg",
          "avg",
          "bit_and",
          "bit_or",
          "bool_and",
          "bool_or",
          "count",
          "every",
          "json_agg",
          "jsonb_agg",
          "json_object_agg",
          "jsonb_agg_obejct",
          "max",
          "min",
          "string_agg",
          "sum",
          "xmlagg");

  Set<String> KNOWN_AGG_STATISTIC =
      Set.of(
          "corr",
          "covar_pop",
          "covar_samp",
          "regr_avgx",
          "regr_count",
          "regr_intercept",
          "regr_r2",
          "regr_slope",
          "regr_sxx",
          "regr_sxy",
          "regr_syy",
          "stddev",
          "stddev_pop",
          "stddev_samp",
          "variance",
          "var_pop",
          "var_samp");

  static boolean isAggregator(String[] pair) {
    final String schemaName = pair[0];
    final String funcName = pair[1];
    return schemaName == null
           && (KNOWN_AGG_BASIC.contains(funcName) || KNOWN_AGG_STATISTIC.contains(funcName));
  }
}
