local TableGen = {}

local function columnList(t, dbType)
    local cols = {}
    for colName in pairs(t) do
        if dbType == "pgsql" then
            table.insert(cols, "\"" .. colName .. "\"")
        else
            table.insert(cols, "`" .. colName .. "`")
        end
    end
    return table.concat(cols, ", ")
end

function TableGen:make(numLines, randSeq, dbType)
    local gen = { numLines = numLines, randSeq = randSeq, dbType = dbType }
    setmetatable(gen, self)
    self.__index = self
    return gen
end

function TableGen:genRow(t, lineNumber)
    local ret = {}
    for n, column in pairs(t.columns) do
        table.insert(ret, column:valueAt(lineNumber, self.randSeq, self.numLines, self.dbType))
    end
    return ret
end

function TableGen:genTable(t, ordinal, callback)
    local prefix
    if self.dbType == "pgsql" then
        prefix = "INSERT INTO \"%s\" (%s) VALUES"
    else
        prefix = "INSERT INTO `%s` (%s) VALUES"
    end

    local insertSql = string.format(prefix, t.tableName, columnList(t.columns, self.dbType))

    local gen = function(lineNum)
        return self:genRow(t, lineNum)
    end

    callback(insertSql, t.tableName, ordinal, gen)
end

return TableGen