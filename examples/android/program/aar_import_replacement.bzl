#this is a macro that replaces the aar_import
# you can use this pattern to provide a different implementation of aar_import
# that does extra work (like jetifying).
# make sure you are using the exposing the same arguments as aar_import!
def macro_instead_of_aar_import(name, aar, deps, exports):
    print("using aar_import replacement for %s" % name)
    native.aar_import(name = name, aar = aar, deps = deps, exports = exports)
