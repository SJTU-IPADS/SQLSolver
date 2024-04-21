local Util = require("testbed.util")
local PosixTime = Util.tryRequire("posix.time")

local function timerStart()
    return PosixTime.clock_gettime(PosixTime.CLOCK_MONOTONIC)
end

local function timerStop(start)
    local _end = PosixTime.clock_gettime(PosixTime.CLOCK_MONOTONIC)
    return (_end.tv_sec - start.tv_sec) * 1000000000 + (_end.tv_nsec - start.tv_nsec)
end

return {
    timerStart = timerStart,
    timerStop = timerStop
}

