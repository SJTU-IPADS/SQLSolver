// Generated from PGParser.g4 by ANTLR 4.8
package wtune.sql.pg.internal;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link PGParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface PGParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link PGParser#sql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSql(PGParser.SqlContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#qname_parser}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQname_parser(PGParser.Qname_parserContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#function_args_parser}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_args_parser(PGParser.Function_args_parserContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#vex_eof}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVex_eof(PGParser.Vex_eofContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#plpgsql_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlpgsql_function(PGParser.Plpgsql_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#plpgsql_function_test_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlpgsql_function_test_list(PGParser.Plpgsql_function_test_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(PGParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#data_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitData_statement(PGParser.Data_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#script_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScript_statement(PGParser.Script_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#script_transaction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScript_transaction(PGParser.Script_transactionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#transaction_mode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransaction_mode(PGParser.Transaction_modeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#lock_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLock_table(PGParser.Lock_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#lock_mode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLock_mode(PGParser.Lock_modeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#script_additional}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScript_additional(PGParser.Script_additionalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#additional_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditional_statement(PGParser.Additional_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#explain_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplain_statement(PGParser.Explain_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#explain_query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplain_query(PGParser.Explain_queryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#execute_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_statement(PGParser.Execute_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#declare_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_statement(PGParser.Declare_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#show_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShow_statement(PGParser.Show_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#explain_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplain_option(PGParser.Explain_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#user_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_name(PGParser.User_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#table_cols_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_cols_list(PGParser.Table_cols_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#table_cols}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_cols(PGParser.Table_colsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#vacuum_mode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVacuum_mode(PGParser.Vacuum_modeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#vacuum_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVacuum_option(PGParser.Vacuum_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#analyze_mode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnalyze_mode(PGParser.Analyze_modeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#boolean_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBoolean_value(PGParser.Boolean_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#fetch_move_direction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFetch_move_direction(PGParser.Fetch_move_directionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#schema_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSchema_statement(PGParser.Schema_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#schema_create}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSchema_create(PGParser.Schema_createContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#schema_alter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSchema_alter(PGParser.Schema_alterContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#schema_drop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSchema_drop(PGParser.Schema_dropContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#schema_import}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSchema_import(PGParser.Schema_importContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_function_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_function_statement(PGParser.Alter_function_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_aggregate_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_aggregate_statement(PGParser.Alter_aggregate_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_extension_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_extension_statement(PGParser.Alter_extension_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_extension_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_extension_action(PGParser.Alter_extension_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#extension_member_object}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtension_member_object(PGParser.Extension_member_objectContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_schema_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_schema_statement(PGParser.Alter_schema_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_language_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_language_statement(PGParser.Alter_language_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_table_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_table_statement(PGParser.Alter_table_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#table_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_action(PGParser.Table_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#column_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_action(PGParser.Column_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#identity_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentity_body(PGParser.Identity_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_identity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_identity(PGParser.Alter_identityContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#storage_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStorage_option(PGParser.Storage_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#validate_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValidate_constraint(PGParser.Validate_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#drop_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_constraint(PGParser.Drop_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#table_deferrable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_deferrable(PGParser.Table_deferrableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#table_initialy_immed}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_initialy_immed(PGParser.Table_initialy_immedContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#function_actions_common}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_actions_common(PGParser.Function_actions_commonContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#function_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_def(PGParser.Function_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_index_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_index_statement(PGParser.Alter_index_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#index_def_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_def_action(PGParser.Index_def_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_default_privileges}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_default_privileges(PGParser.Alter_default_privilegesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#abbreviated_grant_or_revoke}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAbbreviated_grant_or_revoke(PGParser.Abbreviated_grant_or_revokeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#grant_option_for}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrant_option_for(PGParser.Grant_option_forContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_sequence_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_sequence_statement(PGParser.Alter_sequence_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_view_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_view_statement(PGParser.Alter_view_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_event_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_event_trigger(PGParser.Alter_event_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_event_trigger_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_event_trigger_action(PGParser.Alter_event_trigger_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_type_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_type_statement(PGParser.Alter_type_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_domain_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_domain_statement(PGParser.Alter_domain_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_server_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_server_statement(PGParser.Alter_server_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_server_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_server_action(PGParser.Alter_server_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_fts_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_fts_statement(PGParser.Alter_fts_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_fts_configuration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_fts_configuration(PGParser.Alter_fts_configurationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#type_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_action(PGParser.Type_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#set_def_column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_def_column(PGParser.Set_def_columnContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#drop_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_def(PGParser.Drop_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_index_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_index_statement(PGParser.Create_index_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#index_rest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_rest(PGParser.Index_restContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#index_sort}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_sort(PGParser.Index_sortContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#including_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIncluding_index(PGParser.Including_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#index_where}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_where(PGParser.Index_whereContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_extension_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_extension_statement(PGParser.Create_extension_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_language_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_language_statement(PGParser.Create_language_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_event_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_event_trigger(PGParser.Create_event_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_type_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_type_statement(PGParser.Create_type_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_domain_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_domain_statement(PGParser.Create_domain_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_server_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_server_statement(PGParser.Create_server_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_fts_dictionary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_fts_dictionary(PGParser.Create_fts_dictionaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#option_with_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOption_with_value(PGParser.Option_with_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_fts_configuration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_fts_configuration(PGParser.Create_fts_configurationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_fts_template}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_fts_template(PGParser.Create_fts_templateContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_fts_parser}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_fts_parser(PGParser.Create_fts_parserContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_collation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_collation(PGParser.Create_collationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_collation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_collation(PGParser.Alter_collationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#collation_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollation_option(PGParser.Collation_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_user_mapping}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_user_mapping(PGParser.Create_user_mappingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_user_mapping}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_user_mapping(PGParser.Alter_user_mappingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_user_or_role}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_user_or_role(PGParser.Alter_user_or_roleContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_user_or_role_set_reset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_user_or_role_set_reset(PGParser.Alter_user_or_role_set_resetContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#user_or_role_set_reset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_or_role_set_reset(PGParser.User_or_role_set_resetContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_group}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_group(PGParser.Alter_groupContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_group_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_group_action(PGParser.Alter_group_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_tablespace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_tablespace(PGParser.Alter_tablespaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_owner}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_owner(PGParser.Alter_ownerContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_tablespace_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_tablespace_action(PGParser.Alter_tablespace_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_statistics}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_statistics(PGParser.Alter_statisticsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_foreign_data_wrapper}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_foreign_data_wrapper(PGParser.Alter_foreign_data_wrapperContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_foreign_data_wrapper_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_foreign_data_wrapper_action(PGParser.Alter_foreign_data_wrapper_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_operator_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_operator_statement(PGParser.Alter_operator_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_operator_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_operator_action(PGParser.Alter_operator_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#operator_set_restrict_join}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperator_set_restrict_join(PGParser.Operator_set_restrict_joinContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#drop_user_mapping}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_user_mapping(PGParser.Drop_user_mappingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#drop_owned}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_owned(PGParser.Drop_ownedContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#drop_operator_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_operator_statement(PGParser.Drop_operator_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#target_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTarget_operator(PGParser.Target_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#domain_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDomain_constraint(PGParser.Domain_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_transform_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_transform_statement(PGParser.Create_transform_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_access_method}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_access_method(PGParser.Create_access_methodContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_user_or_role}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_user_or_role(PGParser.Create_user_or_roleContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#user_or_role_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_or_role_option(PGParser.User_or_role_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#user_or_role_option_for_alter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_or_role_option_for_alter(PGParser.User_or_role_option_for_alterContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#user_or_role_or_group_common_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_or_role_or_group_common_option(PGParser.User_or_role_or_group_common_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#user_or_role_common_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_or_role_common_option(PGParser.User_or_role_common_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#user_or_role_or_group_option_for_create}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_or_role_or_group_option_for_create(PGParser.User_or_role_or_group_option_for_createContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_group}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_group(PGParser.Create_groupContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#group_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroup_option(PGParser.Group_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_tablespace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_tablespace(PGParser.Create_tablespaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_statistics}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_statistics(PGParser.Create_statisticsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_foreign_data_wrapper}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_foreign_data_wrapper(PGParser.Create_foreign_data_wrapperContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#option_without_equal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOption_without_equal(PGParser.Option_without_equalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_operator_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_operator_statement(PGParser.Create_operator_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#operator_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperator_name(PGParser.Operator_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#operator_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperator_option(PGParser.Operator_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_aggregate_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_aggregate_statement(PGParser.Create_aggregate_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#aggregate_param}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAggregate_param(PGParser.Aggregate_paramContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#set_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_statement(PGParser.Set_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#set_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_action(PGParser.Set_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#session_local_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSession_local_option(PGParser.Session_local_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#set_statement_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_statement_value(PGParser.Set_statement_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_rewrite_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_rewrite_statement(PGParser.Create_rewrite_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#rewrite_command}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRewrite_command(PGParser.Rewrite_commandContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_trigger_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_trigger_statement(PGParser.Create_trigger_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#trigger_referencing}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrigger_referencing(PGParser.Trigger_referencingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#when_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhen_trigger(PGParser.When_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#rule_common}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRule_common(PGParser.Rule_commonContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#rule_member_object}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRule_member_object(PGParser.Rule_member_objectContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#columns_permissions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumns_permissions(PGParser.Columns_permissionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#table_column_privileges}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_column_privileges(PGParser.Table_column_privilegesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#permissions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPermissions(PGParser.PermissionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#permission}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPermission(PGParser.PermissionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#other_rules}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOther_rules(PGParser.Other_rulesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#grant_to_rule}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrant_to_rule(PGParser.Grant_to_ruleContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#revoke_from_cascade_restrict}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRevoke_from_cascade_restrict(PGParser.Revoke_from_cascade_restrictContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#roles_names}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoles_names(PGParser.Roles_namesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#role_name_with_group}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRole_name_with_group(PGParser.Role_name_with_groupContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#comment_on_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComment_on_statement(PGParser.Comment_on_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#security_label}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSecurity_label(PGParser.Security_labelContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#comment_member_object}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComment_member_object(PGParser.Comment_member_objectContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#label_member_object}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabel_member_object(PGParser.Label_member_objectContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_function_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_function_statement(PGParser.Create_function_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_funct_params}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_funct_params(PGParser.Create_funct_paramsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#transform_for_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransform_for_type(PGParser.Transform_for_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#function_ret_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_ret_table(PGParser.Function_ret_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#function_column_name_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_column_name_type(PGParser.Function_column_name_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#function_parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_parameters(PGParser.Function_parametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#function_args}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_args(PGParser.Function_argsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#agg_order}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAgg_order(PGParser.Agg_orderContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#character_string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCharacter_string(PGParser.Character_stringContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#function_arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_arguments(PGParser.Function_argumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#argmode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgmode(PGParser.ArgmodeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_sequence_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_sequence_statement(PGParser.Create_sequence_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#sequence_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSequence_body(PGParser.Sequence_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#signed_number_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSigned_number_literal(PGParser.Signed_number_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#signed_numerical_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSigned_numerical_literal(PGParser.Signed_numerical_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#sign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSign(PGParser.SignContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_schema_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_schema_statement(PGParser.Create_schema_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_policy_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_policy_statement(PGParser.Create_policy_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_policy_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_policy_statement(PGParser.Alter_policy_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#drop_policy_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_policy_statement(PGParser.Drop_policy_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_subscription_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_subscription_statement(PGParser.Create_subscription_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_subscription_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_subscription_statement(PGParser.Alter_subscription_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_subscription_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_subscription_action(PGParser.Alter_subscription_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_cast_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_cast_statement(PGParser.Create_cast_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#drop_cast_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_cast_statement(PGParser.Drop_cast_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_operator_family_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_operator_family_statement(PGParser.Create_operator_family_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_operator_family_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_operator_family_statement(PGParser.Alter_operator_family_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#operator_family_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperator_family_action(PGParser.Operator_family_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#add_operator_to_family}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_operator_to_family(PGParser.Add_operator_to_familyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#drop_operator_from_family}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_operator_from_family(PGParser.Drop_operator_from_familyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#drop_operator_family_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_operator_family_statement(PGParser.Drop_operator_family_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_operator_class_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_operator_class_statement(PGParser.Create_operator_class_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_operator_class_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_operator_class_option(PGParser.Create_operator_class_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_operator_class_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_operator_class_statement(PGParser.Alter_operator_class_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#drop_operator_class_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_operator_class_statement(PGParser.Drop_operator_class_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_conversion_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_conversion_statement(PGParser.Create_conversion_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_conversion_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_conversion_statement(PGParser.Alter_conversion_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_publication_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_publication_statement(PGParser.Create_publication_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_publication_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_publication_statement(PGParser.Alter_publication_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_publication_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_publication_action(PGParser.Alter_publication_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#only_table_multiply}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOnly_table_multiply(PGParser.Only_table_multiplyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_trigger_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_trigger_statement(PGParser.Alter_trigger_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alter_rule_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_rule_statement(PGParser.Alter_rule_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#copy_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_statement(PGParser.Copy_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#copy_from_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_from_statement(PGParser.Copy_from_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#copy_to_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_to_statement(PGParser.Copy_to_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#copy_option_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_option_list(PGParser.Copy_option_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#copy_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopy_option(PGParser.Copy_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_view_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_view_statement(PGParser.Create_view_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#if_exists}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_exists(PGParser.If_existsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#if_not_exists}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_not_exists(PGParser.If_not_existsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#view_columns}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitView_columns(PGParser.View_columnsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#with_check_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWith_check_option(PGParser.With_check_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_table_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_statement(PGParser.Create_table_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_table_as_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table_as_statement(PGParser.Create_table_as_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#create_foreign_table_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_foreign_table_statement(PGParser.Create_foreign_table_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#define_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefine_table(PGParser.Define_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#define_partition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefine_partition(PGParser.Define_partitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#for_values_bound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_values_bound(PGParser.For_values_boundContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#partition_bound_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_bound_spec(PGParser.Partition_bound_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#partition_bound_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_bound_part(PGParser.Partition_bound_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#define_columns}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefine_columns(PGParser.Define_columnsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#define_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefine_type(PGParser.Define_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#partition_by}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_by(PGParser.Partition_byContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#partition_method}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_method(PGParser.Partition_methodContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#partition_column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_column(PGParser.Partition_columnContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#define_server}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefine_server(PGParser.Define_serverContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#define_foreign_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefine_foreign_options(PGParser.Define_foreign_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#foreign_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeign_option(PGParser.Foreign_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#foreign_option_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeign_option_name(PGParser.Foreign_option_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#list_of_type_column_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitList_of_type_column_def(PGParser.List_of_type_column_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#table_column_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_column_def(PGParser.Table_column_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#table_of_type_column_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_of_type_column_def(PGParser.Table_of_type_column_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#table_column_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_column_definition(PGParser.Table_column_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#like_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLike_option(PGParser.Like_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#constraint_common}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraint_common(PGParser.Constraint_commonContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#constr_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstr_body(PGParser.Constr_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#all_op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAll_op(PGParser.All_opContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#all_simple_op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAll_simple_op(PGParser.All_simple_opContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#op_chars}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOp_chars(PGParser.Op_charsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#index_parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_parameters(PGParser.Index_parametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#names_in_parens}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNames_in_parens(PGParser.Names_in_parensContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#names_references}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNames_references(PGParser.Names_referencesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#storage_parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStorage_parameter(PGParser.Storage_parameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#storage_parameter_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStorage_parameter_option(PGParser.Storage_parameter_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#storage_parameter_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStorage_parameter_name(PGParser.Storage_parameter_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#with_storage_parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWith_storage_parameter(PGParser.With_storage_parameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#storage_parameter_oid}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStorage_parameter_oid(PGParser.Storage_parameter_oidContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#on_commit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_commit(PGParser.On_commitContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#table_space}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_space(PGParser.Table_spaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAction(PGParser.ActionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#owner_to}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOwner_to(PGParser.Owner_toContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#rename_to}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRename_to(PGParser.Rename_toContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#set_schema}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_schema(PGParser.Set_schemaContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#table_column_privilege}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_column_privilege(PGParser.Table_column_privilegeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#usage_select_update}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsage_select_update(PGParser.Usage_select_updateContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#partition_by_columns}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_by_columns(PGParser.Partition_by_columnsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#cascade_restrict}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCascade_restrict(PGParser.Cascade_restrictContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#collate_identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollate_identifier(PGParser.Collate_identifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#indirection_var}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndirection_var(PGParser.Indirection_varContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#dollar_number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDollar_number(PGParser.Dollar_numberContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#indirection_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndirection_list(PGParser.Indirection_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#indirection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndirection(PGParser.IndirectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#drop_function_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_function_statement(PGParser.Drop_function_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#drop_trigger_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_trigger_statement(PGParser.Drop_trigger_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#drop_rule_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_rule_statement(PGParser.Drop_rule_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#drop_statements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_statements(PGParser.Drop_statementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#if_exist_names_restrict_cascade}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_exist_names_restrict_cascade(PGParser.If_exist_names_restrict_cascadeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#id_token}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitId_token(PGParser.Id_tokenContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(PGParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#identifier_nontype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier_nontype(PGParser.Identifier_nontypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#col_label}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCol_label(PGParser.Col_labelContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#tokens_nonreserved}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTokens_nonreserved(PGParser.Tokens_nonreservedContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#tokens_nonreserved_except_function_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTokens_nonreserved_except_function_type(PGParser.Tokens_nonreserved_except_function_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#tokens_reserved_except_function_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTokens_reserved_except_function_type(PGParser.Tokens_reserved_except_function_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#tokens_reserved}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTokens_reserved(PGParser.Tokens_reservedContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#tokens_nonkeyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTokens_nonkeyword(PGParser.Tokens_nonkeywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#schema_qualified_name_nontype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSchema_qualified_name_nontype(PGParser.Schema_qualified_name_nontypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#type_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_list(PGParser.Type_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#data_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitData_type(PGParser.Data_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#array_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_type(PGParser.Array_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#predefined_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredefined_type(PGParser.Predefined_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#interval_field}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterval_field(PGParser.Interval_fieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#type_length}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_length(PGParser.Type_lengthContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#precision_param}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrecision_param(PGParser.Precision_paramContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#vex}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVex(PGParser.VexContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#vex_b}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVex_b(PGParser.Vex_bContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOp(PGParser.OpContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#all_op_ref}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAll_op_ref(PGParser.All_op_refContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#datetime_overlaps}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatetime_overlaps(PGParser.Datetime_overlapsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#value_expression_primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue_expression_primary(PGParser.Value_expression_primaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#unsigned_value_specification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnsigned_value_specification(PGParser.Unsigned_value_specificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#unsigned_numeric_literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnsigned_numeric_literal(PGParser.Unsigned_numeric_literalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#truth_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTruth_value(PGParser.Truth_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#case_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_expression(PGParser.Case_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#cast_specification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCast_specification(PGParser.Cast_specificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#function_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_call(PGParser.Function_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#vex_or_named_notation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVex_or_named_notation(PGParser.Vex_or_named_notationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#pointer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointer(PGParser.PointerContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#function_construct}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_construct(PGParser.Function_constructContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#extract_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtract_function(PGParser.Extract_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#system_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSystem_function(PGParser.System_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#date_time_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDate_time_function(PGParser.Date_time_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#string_value_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString_value_function(PGParser.String_value_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#xml_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_function(PGParser.Xml_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#xml_table_column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_table_column(PGParser.Xml_table_columnContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#comparison_mod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparison_mod(PGParser.Comparison_modContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#filter_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilter_clause(PGParser.Filter_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#window_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindow_definition(PGParser.Window_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#frame_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrame_clause(PGParser.Frame_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#frame_bound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrame_bound(PGParser.Frame_boundContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#array_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_expression(PGParser.Array_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#array_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_elements(PGParser.Array_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#array_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_element(PGParser.Array_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#type_coercion}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_coercion(PGParser.Type_coercionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#schema_qualified_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSchema_qualified_name(PGParser.Schema_qualified_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#set_qualifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_qualifier(PGParser.Set_qualifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#table_subquery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_subquery(PGParser.Table_subqueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#select_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_stmt(PGParser.Select_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#after_ops}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAfter_ops(PGParser.After_opsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#select_stmt_no_parens}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_stmt_no_parens(PGParser.Select_stmt_no_parensContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#with_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWith_clause(PGParser.With_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#with_query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWith_query(PGParser.With_queryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#select_ops}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_ops(PGParser.Select_opsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#select_ops_no_parens}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_ops_no_parens(PGParser.Select_ops_no_parensContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#select_primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_primary(PGParser.Select_primaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#select_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_list(PGParser.Select_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#select_sublist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_sublist(PGParser.Select_sublistContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#into_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInto_table(PGParser.Into_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#from_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrom_item(PGParser.From_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#from_primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrom_primary(PGParser.From_primaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#alias_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlias_clause(PGParser.Alias_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#from_function_column_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrom_function_column_def(PGParser.From_function_column_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#groupby_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupby_clause(PGParser.Groupby_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#grouping_element_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrouping_element_list(PGParser.Grouping_element_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#grouping_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrouping_element(PGParser.Grouping_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#values_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValues_stmt(PGParser.Values_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#values_values}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValues_values(PGParser.Values_valuesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#orderby_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderby_clause(PGParser.Orderby_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#sort_specifier_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSort_specifier_list(PGParser.Sort_specifier_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#sort_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSort_specifier(PGParser.Sort_specifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#order_specification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrder_specification(PGParser.Order_specificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#null_ordering}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNull_ordering(PGParser.Null_orderingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#insert_stmt_for_psql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_stmt_for_psql(PGParser.Insert_stmt_for_psqlContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#insert_columns}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_columns(PGParser.Insert_columnsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#indirection_identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndirection_identifier(PGParser.Indirection_identifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#conflict_object}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConflict_object(PGParser.Conflict_objectContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#conflict_action}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConflict_action(PGParser.Conflict_actionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#delete_stmt_for_psql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDelete_stmt_for_psql(PGParser.Delete_stmt_for_psqlContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#update_stmt_for_psql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate_stmt_for_psql(PGParser.Update_stmt_for_psqlContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#update_set}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate_set(PGParser.Update_setContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#notify_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotify_stmt(PGParser.Notify_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#truncate_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTruncate_stmt(PGParser.Truncate_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#identifier_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier_list(PGParser.Identifier_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#anonymous_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnonymous_block(PGParser.Anonymous_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#comp_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComp_options(PGParser.Comp_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#function_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_block(PGParser.Function_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#start_label}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStart_label(PGParser.Start_labelContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#declarations}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclarations(PGParser.DeclarationsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclaration(PGParser.DeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#type_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_declaration(PGParser.Type_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#arguments_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments_list(PGParser.Arguments_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#data_type_dec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitData_type_dec(PGParser.Data_type_decContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#exception_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitException_statement(PGParser.Exception_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#function_statements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_statements(PGParser.Function_statementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#function_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_statement(PGParser.Function_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#base_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBase_statement(PGParser.Base_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#var}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVar(PGParser.VarContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#diagnostic_option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDiagnostic_option(PGParser.Diagnostic_optionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#perform_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPerform_stmt(PGParser.Perform_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#assign_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssign_stmt(PGParser.Assign_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#execute_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_stmt(PGParser.Execute_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#control_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitControl_statement(PGParser.Control_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#cursor_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_statement(PGParser.Cursor_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#option}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOption(PGParser.OptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#transaction_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransaction_statement(PGParser.Transaction_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#message_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMessage_statement(PGParser.Message_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#log_level}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLog_level(PGParser.Log_levelContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#raise_using}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRaise_using(PGParser.Raise_usingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#raise_param}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRaise_param(PGParser.Raise_paramContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#return_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn_stmt(PGParser.Return_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#loop_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoop_statement(PGParser.Loop_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#loop_start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoop_start(PGParser.Loop_startContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#using_vex}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsing_vex(PGParser.Using_vexContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#if_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_statement(PGParser.If_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#case_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_statement(PGParser.Case_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PGParser#plpgsql_query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlpgsql_query(PGParser.Plpgsql_queryContext ctx);
}