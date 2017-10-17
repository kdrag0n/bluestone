function calc(c, last)
    local sandbox = setmetatable({}, {__index = math})
    sandbox['while'] = error

    local fn, err = load(c .. '\nreturn (' .. last .. ')', 'calculator', 't', sandbox)
    if not fn then return err end

    local success, result = pcall(fn)
    return result
end