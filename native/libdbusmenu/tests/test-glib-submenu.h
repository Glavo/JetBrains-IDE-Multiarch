/*
A test for libdbusmenu to ensure its quality.

Copyright 2009 Canonical Ltd.

Authors:
    Ted Gould <ted@canonical.com>

This program is free software: you can redistribute it and/or modify it 
under the terms of the GNU General Public License version 3, as published 
by the Free Software Foundation.

This program is distributed in the hope that it will be useful, but 
WITHOUT ANY WARRANTY; without even the implied warranties of 
MERCHANTABILITY, SATISFACTORY QUALITY, or FITNESS FOR A PARTICULAR 
PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along 
with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

typedef struct _layout_t layout_t;
struct _layout_t {
	gint id;
	layout_t * submenu;
};

layout_t submenu_l2[] = {
	{id: 6, submenu: NULL},
	{id: 7, submenu: NULL},
	{id: 8, submenu: NULL},
	{id: -1, submenu: NULL}
};

layout_t submenu[] = {
	{id: 2, submenu: submenu_l2},
	{id: 3, submenu: submenu_l2},
	{id: -1, submenu: NULL}
};

layout_t no_submenu[] = {
	{id: 4, submenu: NULL},
	{id: 5, submenu: NULL},
	{id: -1, submenu: NULL}
};

layout_t layouts[] = {
	{id: 1, submenu: no_submenu},
	{id: 1, submenu: submenu},
	{id: 1, submenu: no_submenu},
	{id: 1, submenu: submenu},
	{id: -1, submenu: NULL}
};

