local Util = require('testbed.util')
local Exec = require('testbed.exec')
local Inspect = require('inspect')

local function flushOne(stmt)
    if not stmt.latencies then
        return
    end
    table.sort(stmt.latencies)

    local p50 = Util.percentile(stmt.latencies, 0.5)
    local p90 = Util.percentile(stmt.latencies, 0.9)
    local p99 = Util.percentile(stmt.latencies, 0.99)

    io.write(('%d;%d;%d;%d\n'):format(stmt.stmtId, p50, p90, p99))
end

local function flush(stmts, start, stop)
    start = start or 1
    stop = stop or #stmts

    for i = start, stop do
        flushOne(stmts[i])
    end
end

local function markSlow(stmt, elapsed)
    if elapsed >= 10000000000 then
        -- 10s
        stmt.isSlow = math.max(3, stmt.isSlow or 0)
    elseif elapsed >= 1000000000 then
        -- 1s
        stmt.isSlow = math.max(2, stmt.isSlow or 0)
    elseif elapsed >= 100000000 then
        -- 0.1s
        stmt.isSlow = math.max(1, stmt.isSlow or 0)
    end
end

local function throttleSlow(stmt)
    return (stmt.isSlow == 1 and stmt.runs >= 20)
            or (stmt.isSlow == 2 and stmt.runs >= 3)
            or (stmt.isSlow == 3 and stmt.runs >= 1)
end

local function throttleHang(stmt, sqlsolver)
    -- shopizer-3,39,60,104 hangs after several runs, don't know why, workaround for now
    if sqlsolver.app ~= 'shopizer' then
        return false
    end

    if stmt.sql:match(".*DISTINCT.*") and stmt.sql:len() >= 3000 then
        return stmt.runs and stmt.runs >= 1
    end
    return false
end

local function evalStmt(stmt, sqlsolver)
    local paramGen = sqlsolver.paramGen
    local args = paramGen:produce(stmt, paramGen:randomLine())
    local status, elapsed, value = Exec(stmt, args, sqlsolver)

    stmt.runs = 1 + (stmt.runs or 0)
    stmt.latencies = stmt.latencies or {}

    if not status then
        elapsed = 25000000000
        --error(value)
    end

    table.insert(stmt.latencies, elapsed)
    markSlow(stmt, elapsed)

    value = nil
    collectgarbage()
end

local function evalStmts(stmts, sqlsolver)
    Util.setTimeout(25000, sqlsolver.con, sqlsolver.dbType)

    local seq = Util.iterate(#stmts)
    local totalTimes = sqlsolver.times
    local waterMarker = 0

    local filter = sqlsolver.stmtFilter

    if sqlsolver.dump then
        Util.log('[Sample] writing to file\n', 5)
        io.output(sqlsolver:appFile('eval.' .. sqlsolver.tag, "w"))
    end

    Util.log(('[Eval] %d statements to eval, %d times for each\n'):format(#stmts, sqlsolver.times), 1)
    Util.log('[Eval] ', 1)

    for pass = 1, totalTimes do
        local curMarker = math.floor((pass / totalTimes) * 10)
        -- progress bar
        if curMarker ~= waterMarker then
            Util.log('.', 1)
            waterMarker = curMarker
        end

        Util.shuffle(seq)

        for _, i in ipairs(seq) do
            local stmt = stmts[i]

            if (not filter or filter(stmt))
                    and not throttleSlow(stmt)
                    and not throttleHang(stmt, sqlsolver) then
                local status, value = pcall(evalStmt, stmt, sqlsolver)

                if not status then
                    Util.log(('\n[Eval] error: %s-%s @ %d %s\n'):format(sqlsolver.app, stmt.stmtId, pass, Inspect(value)), 1)
                    error(value)
                end

            end
        end
    end

    Util.log('\n', 1)

    flush(stmts)
    -- reset output
    io.output():flush()
    io.output():close()
    io.output(io.stdout)
end

return evalStmts
