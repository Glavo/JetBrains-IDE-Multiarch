#!/usr/bin/python

# A test for libdbusmenu to ensure its quality. This is the Python GI version
# of test-gtk-shortcut-client.c
#
# Copyright 2011 Canonical Ltd.
# Authors:
#   Martin Pitt <martin.pitt@ubuntu.com>

import sys
import gobject
from gi.repository import Gtk, DbusmenuGtk
Gtk.require_version('2.0')

passed = True
main_loop = gobject.MainLoop()

def timer_func(data):
    passed = True
    main_loop.quit()
    return False

# main
print 'Building Window'
window = Gtk.Window(type=Gtk.WindowType.TOPLEVEL)
menubar = Gtk.MenuBar()
menuitem = Gtk.MenuItem(label='Test')

dmenu = DbusmenuGtk.Menu(dbus_name='glib.label.test', dbus_object='/org/test')
dclient = dmenu.get_client()
agroup = Gtk.AccelGroup()
dclient.set_accel_group(agroup)

menuitem.set_submenu(dmenu)
menuitem.show()
menubar.append(menuitem)
menubar.show()
window.add(menubar)
window.set_title('libdbusmenu-gtk test')
window.add_accel_group(agroup)
window.show_all()

gobject.timeout_add_seconds(10, timer_func, window)

print 'Entering Mainloop'
main_loop.run()

if passed:
    print 'Quiting'
else:
    print "Quiting as we're a failure"
    sys.exit(1)
