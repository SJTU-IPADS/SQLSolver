local Inspect = require("inspect")
local Util = require("testbed.util")
local RandGen = require("testbed.randgen")
local RandSeq = require("testbed.randseq")
local Schema = require("testbed.schema")
local Workload = require("testbed.workload")
local ParamGen = require("testbed.paramgen")
local Prepare = require("testbed.prepare")
local Sample = require("testbed.sample")
local Eval = require("testbed.eval")
local Verify = require('testbed.verify')
local Compare = require('testbed.compare')

local Sqlsolver = {}

local PROFILES = {
    sample = {
        tag = 'notag',
        schema = 'opt',
        workload = 'base',
        randDist = 'uniform',
        randSeq = 'typed',
        rows = 10000,
    },
    evalBase = {
        tag = 'base',
        schema = 'base',
        workload = 'base',
        randDist = 'uniform',
        randSeq = 'typed',
        rows = 10000,
        times = 100,
    },
    evalIndex = {
        tag = 'index',
        schema = 'opt',
        workload = 'index',
        randDist = 'uniform',
        randSeq = 'typed',
        rows = 10000,
        times = 100,
    },
    evalOpt = {
        tag = 'opt',
        schema = 'opt',
        workload = 'opt',
        randDist = 'uniform',
        randSeq = 'typed',
        rows = 10000,
        times = 100,
    },
    verify = {
        tag = 'notag',
        schema = 'opt',
        workload = 'verify',
        randDist = 'uniform',
        randSeq = 'random',
        rows = 100,
    },
}

local function tableFilter(type, value)
    if type == "continue" then
        return function(ordinal, t)
            return ordinal >= value
        end

    elseif type == "target" then
        local target = {}
        for tableName in value:gmatch("(.-),") do
            target[tableName:lower()] = true
        end
        return function(ordinal, t)
            return target[t.tableName]
        end
    end
end

local function stmtFilter(type, value)
    if type == "continue" then
        return function(stmt)
            return stmt.stmtId >= value
        end

    elseif type == "target" then
        local target = {}
        for id in value:gmatch("(.-),") do
            if tonumber(id) then
                target[tonumber(id)] = true
            end
        end
        return function(stmt)
            return target[stmt.stmtId]
        end
    end
end

local function indexFilter(type, value)
    if type == "continue" then
        return function(stmt)
            return stmt.index >= value
        end

    elseif type == "target" then
        local target = {}
        for id in value:gmatch("(.-),") do
            if tonumber(id) then
                target[tonumber(id)] = true
            end
        end
        return function(stmt)
            return target[stmt.index]
        end
    end
end

local function lines(value)
    if not value then
        return {}
    end
    local target = {}
    for line in value:gmatch("(.-),") do
        if tonumber(line) then
            table.insert(target, tonumber(line))
        end
    end
    return target
end

function Sqlsolver:loadSchema()
    local fileName = string.format("%s.%s_schema", self.app, self.schemaTag)
    local schemaDesc = Util.tryRequire(fileName)
    if schemaDesc then
        return Schema(self.app):buildFrom(schemaDesc)
    else
        return nil
    end
end

function Sqlsolver:loadWorkload()
    local fileName = string.format("%s.%s_workload", self.app, self.workloadTag)
    local workloadDesc = Util.tryRequire(fileName)
    if workloadDesc then
        return Workload(self.app, self.workloadTag):buildFrom(workloadDesc)
    else
        return nil
    end
end

function Sqlsolver:enableProfile(profileName)
    local profile = PROFILES[profileName]
    if profile then
        for k, v in pairs(profile) do
            self[k] = v
        end
    end
end

function Sqlsolver:cleanOptions(options)
    for k, v in pairs(options) do
        if v == "" then
            options[k] = nil
        end
    end
    return options
end

function Sqlsolver:initOptions(options)
    options = self:cleanOptions(options)
    if options.profile then
        self:enableProfile(options.profile)
    end

    self.app = options.app
    self.dbType = options.dbType or (sysbench and sysbench.opt.db_driver or "mysql")
    self.tag = options.tag or self.tag or "notag"
    self.schemaTag = options.schema or self.schema or "base"
    self.schema = self:loadSchema()
    self.workloadTag = options.workload or self.workload or "base"
    self.workload = self:loadWorkload()
    if not self.workload then
        self.workloadTag = 'base'
        self.workload = self:loadWorkload()
    end
    self.rows = tonumber(options.rows or self.rows or 10000)
    self.times = tonumber(options.times or self.times or 100)
    self.randDist = RandGen(options.randDist or self.randDist or "uniform")
    self.randSeq = RandSeq(self.randDist, self.rows, options.randSeq or self.randSeq or "typed")
    self.paramGen = ParamGen(self.rows, self)
    self.dump = options.dump == 'true' or options.dump == 'yes'
    self.lines = lines(options.lines)
    self.base = options.base and tonumber(options.base)

    if options.continue then
        self.tableFilter = tableFilter("continue", tonumber(options.continue))
        self.stmtFilter = stmtFilter("continue", tonumber(options.continue))
        self.indexFilter = indexFilter("continue", tonumber(options.continue))
    elseif options.targets then
        self.tableFilter = tableFilter("target", options.targets)
        self.stmtFilter = stmtFilter("target", options.targets)
        self.indexFilter = indexFilter("target", options.targets)
    end
end

function Sqlsolver:initConn()
    if not sysbench then
        return
    end

    local drv = sysbench.sql.driver()
    local con = drv:connect()

    self.sysbench = sysbench
    self.drv = drv
    self.con = con

    if sysbench.opt.db_driver ~= "pgsql" then
        con:query("SET FOREIGN_KEY_CHECKS=0")
        con:query("SET UNIQUE_CHECKS=0")
        self.db = sysbench.opt.mysql_db
    else
        con:query("SET session_replication_role='replica'")
        self.db = sysbench.opt.pgsql_db
    end

end

local predefined = {
    app = "broadleaf",
    profile = "base",
    dbType = "mysql",
    tables = "blc_order_item_adjustment,",
    rows = 3
}

function Sqlsolver:init()
    local options = sysbench and sysbench.opt or predefined
    self:initOptions(options)
    if sysbench and sysbench.cmdline.command ~= 'param' then
        self:initConn()
    end
    return self
end

function Sqlsolver:make()
    local o = {}
    setmetatable(o, self)
    self.__index = self
    return o
end

function Sqlsolver:getTable(tableName)
    return self.schema:getTable(tableName)
end

function Sqlsolver:getColumn(tableName, columnName)
    return self:getTable(tableName):getColumn(columnName)
end

function Sqlsolver:getColumnValue(tableName, columnName, lineNum)
    return self:getColumn(tableName, columnName)
               :valueAt(lineNum, self.randSeq, self.rows, self.dbType)
end

function Sqlsolver:redirect(lineNum)
    return self.schema:redirect(lineNum, self.rows)
end

function Sqlsolver:appFile(fileName, flag)
    return io.open(("%s/%s"):format(self.app, fileName), flag)
end

function Sqlsolver:doPrepare()
    Util.log(("[Prepare] Start to prepare database for %s\n"):format(self.app), 1)
    Util.log(("[Prepare] app: %s, schema: %s, db: %s\n"):format(self.app, self.schemaTag, self.db), 2)
    Util.log(("[Prepare] rows: %d, dist: %s, seq: %s\n"):format(self.rows, self.randDist.type, self.randSeq.type), 2)
    Prepare(self.schema.tables, self)
end

function Sqlsolver:doSample()
    Util.log(("[Sample] Start to sample %s\n"):format(self.app), 1)
    Util.log(("[Sample] app: %s, schema: %s, db: %s, workload: %s\n"):format(self.app, self.schemaTag, self.db, self.workloadTag), 2)
    Util.log(("[Sample] rows: %d, dist: %s, seq: %s\n"):format(self.rows, self.randDist.type, self.randSeq.type), 2)

    Sample(self.workload.stmts, self)
end

function Sqlsolver:doEval()
    Util.log(("[Eval] Start to eval %s\n"):format(self.app), 1)
    Util.log(("[Eval] app: %s, schema: %s, db: %s, workload: %s\n"):format(self.app, self.schemaTag, self.db, self.workloadTag), 2)
    Util.log(("[Eval] rows: %d, dist: %s, seq: %s\n"):format(self.rows, self.randDist.type, self.randSeq.type), 2)

    Eval(self.workload.stmts, self)
end

function Sqlsolver:doParam()
    local filter = self.workloadTag == 'verify' and self.indexFilter or self.stmtFilter

    for _, stmt in ipairs(self.workload.stmts) do
        if not filter or filter(stmt) then
            for _, lineNum in ipairs(self.lines) do
                local args = self.paramGen:produce(stmt, lineNum, false, Util.dumb)
                Util.log(('%s-%d @ %d\n'):format(self.app, stmt.stmtId, lineNum))
                for index, value in ipairs(args) do
                    Util.log(('  [%d] %s\n'):format(index, value), 1)
                end
                Util.log(stmt.sql:format(unpack(args)) .. '\n', 3)
            end
        end
    end
end

function Sqlsolver:doVerify()
    Util.log(("[Verify] Start to verify %s\n"):format(self.app), 2)
    Util.log(("[Verify] app: %s, schema: %s, db: %s, workload: %s\n"):format(self.app, self.schemaTag, self.db, self.workloadTag), 2)
    Util.log(("[Verify] rows: %d, dist: %s, seq: %s\n"):format(self.rows, self.randDist.type, self.randSeq.type), 2)
    Verify(self.workload.stmts, self)
end

function Sqlsolver:doCompare()
    Util.log(("[Compare] Start to compare %s\n"):format(self.app), 2)
    Util.log(("[Compare] app: %s, schema: %s, db: %s, workload: %s\n"):format(self.app, self.schemaTag, self.db, self.workloadTag), 2)
    Util.log(("[Compare] rows: %d, dist: %s, seq: %s, times: %d\n"):format(self.rows, self.randDist.type, self.randSeq.type, self.times), 2)
    Compare(self.workload.stmts, self)
end

local function doPrepare()
    Sqlsolver:make():init():doPrepare()
end

local function doSample()
    Sqlsolver:make():init():doSample()
end

local function doEval()
    Sqlsolver:make():init():doEval()
end

local function doParam()
    Sqlsolver:make():init():doParam()
end

local function doVerify()
    Sqlsolver:make():init():doVerify()
end

local function doCompare()
    Sqlsolver:make():init():doCompare()
end

if sysbench then
    sysbench.cmdline.options = {
        app = { "app name" },
        profile = { "profile name" },
        tag = { "tag" },
        schema = { "schema file" },
        workload = { "workload file" },
        rows = { "#rows" },
        randDist = { "random distribution", "uniform" },
        randSeq = { "type of random sequence", "typed" },
        continue = { "continue populate tables from given index" },
        targets = { "populate given tables" },
        dump = { "whether to dump to file" },
        times = { "how many times is a statement run" },
        lines = { "target lines" },
        base = { "baseline" }
    }
    sysbench.cmdline.commands = {
        prepare = { doPrepare },
        sample = { doSample },
        eval = { doEval },
        param = { doParam },
        verify = { doVerify },
        compare = { doCompare }
    }
    sysbench.hooks.sql_error_ignorable = function(err)
        Util.log(Inspect(err) .. '\n', 1)
        return true
    end
end

return Sqlsolver
