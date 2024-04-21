-- local inspect = require("inspect")
local Dist = require('randomdist')

local Rand = {}

function Rand:make(type)
    local rand = { type = type or self.type }
    setmetatable(rand, self);
    self.__index = self
    return rand
end

function Rand:setSeed(seed)
    math.randomseed(seed)
end

function Rand:forward(step)
    for i = 1, step do
        math.random()
    end
end

function Rand:nextInt(lower, upper)
    return math.random(lower, upper)
end

local Zipf = Rand:make('zipf')

function Zipf:nextInt(lower, upper)
    lower = lower or 1
    upper = upper or 10000

    if upper <= lower then
        return lower
    end

    local range = upper - lower
    if self.range ~= range then
        self.range = range
        self.zipf = Dist.new_zipf(range)
    end
    self.lower = lower
    return self.zipf() + lower
end

local function makeGen(type)
    if type == "uniform" then
        return Rand:make('uniform')
    elseif type == 'zipf' then
        return Zipf:make()
    else
        return assert(false)
    end
end

return makeGen