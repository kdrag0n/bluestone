function calc(c, last)
    local sandbox = setmetatable({}, {__index = math})
    sandbox['while'] = error

    local fn, err = load(c .. '\nreturn (' .. last .. ')', 'calculator', 't', sandbox)
    if not fn then return err end

    local success, result = pcall(fn)
    if not success then return result end
    return result
end