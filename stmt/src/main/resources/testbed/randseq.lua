local Util = require('testbed.util')

local TypedSeq = {}
local TYPE_OFFSET = {
    integral = 1,
    fraction = 2,
    boolean = 3,
    enum = 4,
    string = 5,
    bit_string = 6,
    time = 7,
    blob = 8,
    json = 9,
    geo = 10,
    interval = 11,
    net = 12,
    monetary = 13,
    uuid = 14,
    xml = 15,
    range = 16,
    unclassified = 17
}
local MAX_VALUE = 1000000

function TypedSeq:make(rand, max)
    local gen = { type = 'typed', rand = rand, max = max,
                  nonUniqueCaches = {}, uniqueCaches = {} }
    setmetatable(gen, self)
    self.__index = self
    return gen
end

function TypedSeq:getNonUniqueSeq(dataTypeCat)
    local seq = self.nonUniqueCaches[dataTypeCat]
    if seq then
        return seq
    end

    seq = {}
    self.nonUniqueCaches[dataTypeCat] = seq

    local offset = TYPE_OFFSET[dataTypeCat]
    local rand = self.rand
    for i = 1, self.max do
        rand:setSeed(i)
        rand:forward(offset)
        table.insert(seq, rand:nextInt(1, MAX_VALUE))
    end

    return seq
end

function TypedSeq:getUniqueSeq(dataTypeCat)
    local seq = self.uniqueCaches[dataTypeCat]
    if seq then
        return seq
    end

    seq = {}
    self.uniqueCaches[dataTypeCat] = seq

    for i = 1, self.max do
        table.insert(seq, i)
    end

    local nonUniqueSeq = self:getNonUniqueSeq(dataTypeCat)
    for i = self.max, 1, -1 do
        local j = (nonUniqueSeq[i] % i) + 1
        seq[i], seq[j] = seq[j], seq[i]
    end

    return seq
end

function TypedSeq:getSeq(dataTypeCat, isUnique)
    if isUnique then
        return self:getUniqueSeq(dataTypeCat)
    else
        return self:getNonUniqueSeq(dataTypeCat)
    end
end

function TypedSeq:gen(column, lineNum)
    local dataTypeCat = column.dataType.category:lower()
    local seq = self:getSeq(dataTypeCat, column:uniqueIndex())
    assert(seq)
    return seq[lineNum]
end

local RandSeq = {}

function RandSeq:make(rand, max)
    local gen = { type = 'random', rand = rand, max = max, }
    setmetatable(gen, self)
    self.__index = self
    return gen
end

function RandSeq:gen(column, lineNum)
    self.rand:setSeed(Util.strHash(column.tableName .. column.columnName) + lineNum)
    return self.rand:nextInt(1, MAX_VALUE)
end

local function makeSeq(rand, max, type)
    if type == "typed" then
        return TypedSeq:make(rand, max)
    elseif type == 'random' then
        return RandSeq:make(rand, max)
    else
        assert(false)
        return nil
    end
end

return makeSeq