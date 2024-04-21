local Util = require("testbed.util")
local date = require("date")

local Modifiers = {}
local Funcs = {}

local function determineShift(lineNum, shift)
    if shift then
        return shift
    end
    math.randomseed(lineNum)
    shift = math.random(4) - 2 -- -1, 0, 1, 2
    if shift == 0 then
        return -2
    else
        return shift
    end
end

local function shiftValue(value, lineNum, dbType, shift)
    shift = determineShift(lineNum, shift)

    if type(value) == 'table' then
        return nil
    end
    if type(value) == 'number' then
        return value + shift
    elseif value == 'NULL' then
        return 'NULL'
    elseif value:len() < 19 then
        if value:len() == 1 then
            return value == "'0'" and "'1'" or "'0'"
        else
            local num = tonumber(Util.unquote(value, "'"))
            if num then
                return ("'%0" .. value:len() .. "d'"):format(num + shift)
            else
                return "'0" .. value:sub(2)
            end
        end
    else
        shift = shift < 0 and -1 or 1
        return Util.timeFmt(Util.timeParse(value):addhours(24 * shift), dbType)
    end
end

function Modifiers.column_value(tableName, columnName, position)
    return function(sqlsolver, lineNum, stack)
        for _ = 1, position do
            lineNum = sqlsolver:redirect(lineNum)
        end

        local value = sqlsolver:getColumnValue(tableName, columnName, lineNum)
        stack:push(value)
    end
end

function Modifiers.inverse()
    return function(_, _, stack)
        local val = stack:pop()
        if val == 'NULL' then
            stack:push('0')
        else
            stack:push(-val)
        end
    end
end

function Modifiers.subtract()
    return function(_, _, stack)
        local right = stack:pop()
        local left = stack:pop()
        if left == 'NULL' or right == 'NULL' then
            stack:push('NULL')
            return
        end
        stack:push(left - right)
    end
end

function Modifiers.divide()
    return function(_, _, stack)
        local right = stack:pop()
        local left = stack:pop()
        stack:push(left / right)
    end
end

function Modifiers.add()
    return function(_, _, stack)
        stack:push(stack:pop() + stack:pop())
    end
end

function Modifiers.times()
    return function(_, _, stack)
        stack:push(stack:pop() * stack:pop())
    end
end

function Modifiers.decrease()
    return function(sqlsolver, lineNum, stack)
        local shift = sqlsolver.rows <= 100 and -1 or -10
        stack:push(shiftValue(stack:pop(), lineNum, sqlsolver.dbType, shift))
    end
end

function Modifiers.increase()
    return function(sqlsolver, lineNum, stack)
        local shift = sqlsolver.rows >= 100 and 1 or 10
        stack:push(shiftValue(stack:pop(), lineNum, sqlsolver.dbType, shift))
    end
end

function Modifiers.like(wildcardPrefix, wildcardSuffix)
    return function(_, _, stack)
        local value = stack:pop()
        if value == 'NULL' then
            stack:push(value)
            return
        end
        if wildcardPrefix then
            value = "'%" .. value:sub(2)
        end
        if wildcardSuffix then
            value = value:sub(1, #value - 1) .. "%'"
        end
        stack:push(value)
    end
end

function Modifiers.regex()
    return function(_, _, stack)
        local value = stack:pop()
        if value == 'NULL' then
            stack:push(value)
            return
        end
        stack:push(value:sub(1, #value - 1) .. ".*'")
    end
end

function Modifiers.check_null()
    return function(_, _, stack)
        local value = stack:pop()
        if value ~= "NULL" then
            value = "NOT NULL"
        end
        stack:push(value)
    end
end

function Modifiers.check_bool()
    return function(_, _, stack)
        local value = stack:pop()
        if value == 'TRUE' or value == 1 then
            stack:push('TRUE')
        else
            stack:push('FALSE')
        end
    end
end

function Modifiers.check_null_not()
    return function(_, _, stack)
        local value = stack:pop()
        if value ~= "NULL" then
            value = "NULL"
        else
            value = "NOT NULL"
        end
        stack:push(value)
    end
end

function Modifiers.check_bool_not()
    return function(_, _, stack)
        local value = stack:pop()
        if value == 'TRUE' or value == 1 then
            stack:push('FALSE')
        else
            stack:push('TRUE')
        end
    end
end

function Modifiers.neq()
    return function(sqlsolver, lineNum, stack)
        stack:push(shiftValue(stack:pop(), lineNum, sqlsolver.dbType))
    end
end

function Modifiers.direct_value(value)
    return function(_, _, stack)
        stack:push(value)
    end
end

function Modifiers.make_tuple(count)
    return function(_, _, stack)
        local var = {}
        for i = count, 1, -1 do
            var[i] = stack:pop()
        end
        var.asTuple = true
        stack:push(var)
    end
end

function Modifiers.array_element()
    return function(_, _, stack)
        --local value = stack:pop()
        --local shifted = shiftValue(value)
        --if shifted then
        --    stack:push({ value, shifted })
        --else
        --    stack:push(value)
        --end
    end
end

function Modifiers.tuple_element()
    return function(sqlsolver, lineNum, stack)
        local value = stack:pop()
        local shifted = shiftValue(value, lineNum, sqlsolver.dbType)
        if shifted then
            stack:push({ value, shifted })
        else
            stack:push(value)
        end
    end
end

function Modifiers.matching()
    return function(_, _, _)
        -- don't need to do anything
    end
end

function Modifiers.gen_offset()
    return function(_, _, stack)
        stack:push(0)
    end
end

function Modifiers.invoke_agg(funcName)
    return function(_, _, stack)
        stack:push(1000)
    end
end

function Modifiers.invoke_func(funcName, argCount)
    return function(_, _, stack)
        local args = {}
        for i = argCount, 1, -1 do
            args[i] = stack:pop()
        end
        funcName = Util.unquote(funcName)
        local ret = Funcs[funcName](args)
        if ret then
            stack:push(ret)
        end
    end
end

function Funcs.upper(values)
    if values[1] == 'NULL' then
        return 'NULL'
    end

    return values[1]:upper()
end

function Funcs.lower(values)
    if values[1] == 'NULL' then
        return 'NULL'
    end
    return values[1]:lower()
end

function Funcs.extract(values)
    local unit = values[1]
    local value = values[2]
    if value == 'NULL' then
        return 'NULL'
    end
    local time = Util.timeParse(value)
    return time["get" .. unit:lower()](time)
end

function Funcs.coalesce(values)
    for _, value in ipairs(values) do
        if value ~= "NULL" then
            return value
        end
    end
    return "NULL"
end

function Funcs.string_to_array(values)
    if values[1] == 'NULL' then
        return 'NULL'
    end
    return values[1] -- cheat based on current workload
end

function Funcs.length(values)
    if values[1] == 'NULL' then
        return 'NULL'
    end
    return values[1]:len()
end

local function removeNULL(list)
    local j, n = 1, #list;

    for i = 1, n do
        if (list[i] ~= 'NULL') then
            -- Move i's kept value to j's position, if it's not already there.
            if (i ~= j) then
                list[j] = list[i];
                list[i] = nil;
            end
            j = j + 1; -- Increment position of where we'll place the next kept value.
        else
            list[i] = nil;
        end
    end

    return list;
end

function Funcs.greatest(values)
    values = removeNULL(values)
    table.sort(values, Util.timeCompare)
    return values[#values]
end

function Funcs.now(values)
    return Util.timeNow()
end

function Funcs.datediff(values)
    --local left = Util.timeParse(values[1])
    --local right = values[2] -- cheat based on current workload
    --return date.diff(left, right):spandays()
    return 0
end

function Funcs.year(values)
    if values[1] == 'NULL' then
        return 'NULL'
    end
    return Util.timeParse(values[1]):getyear()
end

function Funcs.month(values)
    if values[1] == 'NULL' then
        return 'NULL'
    end
    return Util.timeParse(values[1]):getmonth()
end

function Funcs.dayofmonth(values)
    if values[1] == 'NULL' then
        return 'NULL'
    end
    return Util.timeParse(values[1]):getday()
end

function Funcs.minute(values)
    if values[1] == 'NULL' then
        return 'NULL'
    end
    return Util.timeParse(values[1]):getminutes()
end

function Funcs.weekday(values)
    if values[1] == 'NULL' then
        return 'NULL'
    end
    return Util.timeParse(values[1]):getweekday()
end

function Funcs.second(values)
    if values[1] == 'NULL' then
        return 'NULL'
    end
    return Util.timeParse(values[1]):getseconds()
end

function Funcs.date_format(values)
    if values[1] == 'NULL' then
        return 'NULL'
    end
    return "'" .. Util.timeParse(values[1]):fmt(values[2]) .. "'"
end

return Modifiers