local Workload = {}
local Statement = {}

function Statement:make(stmtDesc)
    setmetatable(stmtDesc, self)
    self.__index = self
    return stmtDesc
end

function Workload:addStmt(stmtDesc)
    local stmt = Statement:make(stmtDesc)
    table.insert(self.stmts, stmt)
    return stmt
end

function Workload:make(appName, tag)
    local workload = { appName = appName, tag = tag, stmts = {} }
    setmetatable(workload, self)
    self.__index = self
    return workload
end

function Workload:buildFrom(workloadDesc)
    for _, stmtDesc in ipairs(workloadDesc) do
        self:addStmt(stmtDesc)
    end
    return self
end

local function makeWorkload(appName, tag)
    return Workload:make(appName, tag)
end

return makeWorkload