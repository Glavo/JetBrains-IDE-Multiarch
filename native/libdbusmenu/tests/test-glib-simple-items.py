#!/usr/bin/python
# This is the Python GI version of test-glib-simple-items.c

import gobject
from gi.repository import Dbusmenu

dummies = ['Bob', 'Jim', 'Alvin', 'Mary']

def dummy_users(root):
    count = 0
    for user in dummies:
        mi = Dbusmenu.Menuitem()
        print 'Creating item: %d %s' % (mi.get_id(), user)
        print '\tRoot ID:', root.get_id()
        mi.property_set('label', user)
        root.child_add_position(mi, count)
        assert mi.property_get('label') == user
        count += 1

def quititall(mainloop):
    mainloop.quit()
    return False

# main

server = Dbusmenu.Server.new('/test/object')
root_menuitem = Dbusmenu.Menuitem()
server.set_root(root_menuitem)
print 'Root ID:', root_menuitem.get_id()

dummy_users(root_menuitem)

mainloop = gobject.MainLoop()
gobject.timeout_add_seconds(1, quititall, mainloop)
mainloop.run()
