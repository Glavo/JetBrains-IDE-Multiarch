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

layout_t submenu_2[] = {
	{id: 2, submenu: NULL},
	{id: 3, submenu: NULL},
	{id: -1, submenu: NULL}
};
layout_t submenu_3_1[] = {
	{id: 3, submenu: NULL},
	{id: 4, submenu: NULL},
	{id: 5, submenu: NULL},
	{id: -1, submenu: NULL}
};
layout_t submenu_3_2[] = {
	{id: 7, submenu: NULL},
	{id: 8, submenu: NULL},
	{id: 9, submenu: NULL},
	{id: -1, submenu: NULL}
};
layout_t submenu_3[] = {
	{id: 2, submenu: submenu_3_1},
	{id: 6, submenu: submenu_3_2},
	{id: -1, submenu: NULL}
};
layout_t submenu_4_1[] = {
	{id: 6, submenu: NULL},
	{id: -1, submenu: NULL}
};
layout_t submenu_4_2[] = {
	{id: 5, submenu: submenu_4_1},
	{id: -1, submenu: NULL}
};
layout_t submenu_4_3[] = {
	{id: 4, submenu: submenu_4_2},
	{id: -1, submenu: NULL}
};
layout_t submenu_4_4[] = {
	{id: 3, submenu: submenu_4_3},
	{id: -1, submenu: NULL}
};
layout_t submenu_4_5[] = {
	{id: 2, submenu: submenu_4_4},
	{id: -1, submenu: NULL}
};

layout_t layouts[] = {
	{id: 5, submenu: NULL},
	{id: 1, submenu: submenu_2},
	{id: 1, submenu: submenu_3},
	{id: 1, submenu: submenu_4_5},
	{id: -1, submenu: NULL}
};

