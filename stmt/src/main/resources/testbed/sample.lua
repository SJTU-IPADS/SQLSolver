local Util = require("testbed.util")
local Exec = require("testbed.exec")
local Inspect = require("inspect")

local POINTS = { 57, 783, 3722, 6951, 8704 }

local function flushOne(stmt)
    if stmt.samples then
        for point, sample in pairs(stmt.samples) do
            io.write(('>%d;%d;%d\n'):format(point, stmt.stmtId, sample.rows))
            io.write(sample.res)
            io.write('\n')
        end
    end
end

local function flush(stmts, start, stop)
    start = start or 1
    stop = stop or #stmts
    for i = start, stop do
        flushOne(stmts[i])
    end
end

local function sampleStmtAtPoint(stmt, point, sqlsolver)
    local args = sqlsolver.paramGen:produce(stmt, point)
    local status, _, res = Exec(stmt, args, sqlsolver)

    if not status then
        Util.log(('\n[Sample] error: %s-%s @ %s %s\n'):format(sqlsolver.app, stmt.stmtId, point, Inspect(res)), 1)
        error(res)
    end

    local numRows, strRes = Util.stringifyResultSet(res)
    stmt.samples[point] = { rows = numRows, res = strRes }

    return numRows
end

local function sampleStmt(stmt, sqlsolver)
    stmt.samples = {}
    local numRows = 0
    for _, point in ipairs(POINTS) do
        numRows = numRows + sampleStmtAtPoint(stmt, point, sqlsolver)
    end
    return numRows
end

local function sampleStmts(stmts, sqlsolver)
    local filter = sqlsolver.stmtFilter

    if sqlsolver.dump then
        Util.log('[Sample] writing to file\n', 5)
        io.output(sqlsolver:appFile("sample", "w"))
    end

    local totalStmts = #stmts
    Util.log(('[Sample] %d statements to sample\n'):format(totalStmts), 1)
    Util.log('[Sample] ', 1)

    local numRows = 0
    local next = 1
    local waterMarker = 0

    for i, stmt in ipairs(stmts) do
        local curMarker = math.floor((i / totalStmts) * 10)
        -- progress bar
        if curMarker ~= waterMarker then
            Util.log('.', 1)
            waterMarker = curMarker
        end

        if not filter or filter(stmt) then
            numRows = numRows + sampleStmt(stmt, sqlsolver)
            if numRows >= 1000000 then
                -- flush if too large
                flush(stmts, next, i)
                numRows = 0
                next = i + 1
            end
        end
    end

    Util.log('\n', 1)

    flush(stmts, next, #stmts)
    -- reset output
    io.output():flush()
    io.output():close()
    io.output(io.stdout)
end

return sampleStmts