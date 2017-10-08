function calc(code)
    local sandbox = setmetatable({}, {__index = math})

    local fn, err = load('return (' .. code .. ')', 'calculator', 't', sandbox)
    if not fn then return err end

    local success, result = pcall(fn)
    if not success then return result end
    return result
end