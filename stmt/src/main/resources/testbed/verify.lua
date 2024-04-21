local Exec = require('testbed.exec')
local Util = require('testbed.util')

local function sample(stmt, sqlsolver, step, argsOverride, shouldStop)
    local allArgs = {}
    local allResults = {}

    for lineNum = 1, 100, step do
        local args, argsMap = sqlsolver.paramGen:produce(stmt, lineNum, true)

        if argsOverride then
            for id, override in pairs(argsOverride[lineNum]) do
                local current = argsMap[id]
                if current then
                    args[current.index] = override.value
                end
            end
        else
            allArgs[lineNum] = argsMap
        end

        local status, _, rs = Exec(stmt, args, sqlsolver)
        if not status then
            return nil
        end

        local _, str = Util.stringifyResultSet(rs)
        if shouldStop and shouldStop(lineNum, str) then
            return nil
        end

        allResults[lineNum] = str
    end

    return allArgs, allResults
end

local function compareResult(baseResults)
    return function(lineNum, str)
        return baseResults[lineNum] ~= str
    end
end

-- check whether candidates are equivalent to base stmt
local function doVerify(stmts, sqlsolver)
    local baseStmt = stmts[1]
    local filter = sqlsolver.indexFilter

    -- pass 1
    -- compare candidates to base stmt
    local pass1Checked = {}
    local baseArgs, baseResults = sample(baseStmt, sqlsolver, 4)
    local baseVerifier = compareResult(baseResults)

    for i = 2, #stmts do
        local stmt = stmts[i]

        if not filter or filter(stmt) then
            if sample(stmt, sqlsolver, 4, baseArgs, baseVerifier) then
                table.insert(pass1Checked, stmt)
            end
        end
    end

    -- pass 2
    -- compare base stmt to candidates
    local pass2Checked = {}
    for _, stmt in ipairs(pass1Checked) do
        local stmtArgs, stmtResults = sample(stmt, sqlsolver, 20)
        local stmtVerifier = compareResult(stmtResults)

        if sample(baseStmt, sqlsolver, 20, stmtArgs, stmtVerifier) then
            table.insert(pass2Checked, stmt)
        end
    end

    for i = 1, #pass2Checked do
        pass2Checked[i] = pass2Checked[i].index
    end

    print('>' .. table.concat(pass2Checked, ','))
end

return doVerify