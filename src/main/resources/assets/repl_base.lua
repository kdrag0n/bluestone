__code = ''
__last_line = ''
__orig = ''

function __repl_step()
    local fn, err = load(__code .. '\nreturn (' .. __last_line .. ')', 'repl')

    if not fn then
        fn, err = load(__orig, 'repl')
        if not fn then
            return err
        end
    end

    local success, result = pcall(fn)

    return result
end