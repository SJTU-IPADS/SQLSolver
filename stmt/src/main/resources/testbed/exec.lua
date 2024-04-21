local Timer = require('testbed.timer')

local function invoke(con, sql)
    return con:query(sql)
end

local function execute(stmt, args, sqlsolver)
    local con = sqlsolver.con
    local sql = stmt.sql:format(unpack(args))

    local timer = Timer.timerStart()
    local status, res = pcall(invoke, con, sql)
    local elapsed = Timer.timerStop(timer)

    return status, elapsed, res
end

return execute