local Util = require("testbed.util")
local Inspect = require('inspect')

local ParamGen = {}

function ParamGen:randomLine()
    return math.random(self.maxLine)
end

function ParamGen:normalize(p)
    if type(p) == 'number' then
        if p < 0 then
            return ('(%s)'):format(p)
        elseif p == 0 then
            return 0 -- handle wired '-0'
        end
    end

    return p
end

function ParamGen:produceOne(paramDesc, lineNum)
    local stack = Util.Stack:make()

    for _, modifier in ipairs(paramDesc) do
        local status, err = pcall(modifier, self.sqlsolver, lineNum, stack)
        if not status then
            return nil, err
        end
    end

    local warning = stack:size() ~= 1
    local value = stack:pop()

    if type(value) == 'table' then
        local asTuple = value.asTuple
        value = table.concat(value, ', ')
        if asTuple then
            value = '(' .. value .. ')'
        end
    end

    return self:normalize(value), warning
end

function ParamGen:produce(stmt, lineNum, genMap, reportError)
    local args = {}
    local indexedArgs = {}
    reportError = reportError or error

    for i, param in ipairs(stmt.params) do
        local value, warning = self:produceOne(param.mods, lineNum)
        if not value then
            Util.log(('\n[Param] err: %s-%d [%d] %s\n'):format(self.sqlsolver.app, stmt.stmtId, i, Inspect(warning)), 1)
            reportError(value)
            value = '<error>'
        end
        if warning then
            Util.log(('[Param] unbalanced stack: %s-%d [%d]\n'):format(self.sqlsolver.app, stmt.stmtId, i), 1)
        end
        table.insert(args, value)
        if genMap then
            indexedArgs[param.id] = { index = i, value = value }
        end
    end

    return args, indexedArgs
end

function ParamGen:make(maxLine, sqlsolver)
    local gen = { sqlsolver = sqlsolver, maxLine = maxLine }
    setmetatable(gen, self)
    self.__index = self
    return gen
end

local function makeGen(maxLine, sqlsolver)
    return ParamGen:make(maxLine, sqlsolver)
end

return makeGen