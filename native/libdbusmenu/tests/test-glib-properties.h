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


#include <glib.h>

typedef struct _proplayout_t proplayout_t;
struct _proplayout_t {
	gint id;
	gchar ** properties;
	proplayout_t * submenu;
};

gchar * props1[] = {"property1", "value1", "property2", "value2", NULL};
gchar * props2[] = {"property00", "value00", "property01", "value01", "property02", "value02", "property03", "value03", "property04", "value04",
                    "property05", "value05", "property06", "value06", "property07", "value07", "property08", "value08", "property09", "value09",
                    "property10", "value10", "property11", "value11", "property12", "value12", "property13", "value13", "property14", "value14",
                    "property15", "value15", "property16", "value16", "property17", "value17", "property18", "value18", "property19", "value19",
                    "property20", "value20", "property21", "value21", "property22", "value22", "property23", "value23", "property24", "value24",
                    "property25", "value25", "property26", "value26", "property27", "value27", "property28", "value28", "property29", "value29",
                    "property30", "value30", "property31", "value31", "property32", "value32", "property33", "value33", "property34", "value34",
                    "property35", "value35", "property36", "value36", "property37", "value37", "property38", "value38", "property39", "value39",
                    "property40", "value40", "property41", "value41", "property42", "value42", "property43", "value43", "property44", "value44",
                    "property45", "value45", "property46", "value46", "property47", "value47", "property48", "value48", "property49", "value49",
                    "property50", "value50", "property51", "value51", "property52", "value52", "property53", "value53", "property54", "value54",
                    "property55", "value55", "property56", "value56", "property57", "value57", "property58", "value58", "property59", "value59",
                    "property60", "value60", "property61", "value61", "property62", "value62", "property63", "value63", "property64", "value64",
                    "property65", "value65", "property66", "value66", "property67", "value67", "property68", "value68", "property69", "value69",
                    "property70", "value70", "property71", "value71", "property72", "value72", "property73", "value73", "property74", "value74",
                    "property75", "value75", "property76", "value76", "property77", "value77", "property78", "value78", "property79", "value79",
                    "property80", "value80", "property81", "value81", "property82", "value82", "property83", "value83", "property84", "value84",
                    "property85", "value85", "property86", "value86", "property87", "value87", "property88", "value88", "property89", "value89",
                    "property90", "value90", "property91", "value91", "property92", "value92", "property93", "value93", "property94", "value94",
                    "property95", "value95", "property96", "value96", "property97", "value97", "property98", "value98", "property99", "value99",
                    NULL};
gchar * props3[] = {"property name that is really long and will ensure that we can really have long property names, which could be important at some point.",
                    "And a property name that is really long should have a value that is really long, because well, that's an important part of the yin and yang of software testing.",
                    NULL};
gchar * props4[] = {"icon-name", "network-status", "label", "Look at network", "right-column", "10:32", NULL};


proplayout_t submenu_4_1[] = {
	{id: 10, properties: props2, submenu: NULL},
	{id: 11, properties: props2, submenu: NULL},
	{id: 12, properties: props2, submenu: NULL},
	{id: 13, properties: props2, submenu: NULL},
	{id: 14, properties: props2, submenu: NULL},
	{id: 15, properties: props2, submenu: NULL},
	{id: 16, properties: props2, submenu: NULL},
	{id: 17, properties: props2, submenu: NULL},
	{id: 18, properties: props2, submenu: NULL},
	{id: 19, properties: props2, submenu: NULL},
	{id: -1, properties: NULL, submenu: NULL}
};

proplayout_t submenu_4_2[] = {
	{id: 20, properties: props2, submenu: NULL},
	{id: 21, properties: props2, submenu: NULL},
	{id: 22, properties: props2, submenu: NULL},
	{id: 23, properties: props2, submenu: NULL},
	{id: 24, properties: props2, submenu: NULL},
	{id: 25, properties: props2, submenu: NULL},
	{id: 26, properties: props2, submenu: NULL},
	{id: 27, properties: props2, submenu: NULL},
	{id: 28, properties: props2, submenu: NULL},
	{id: 29, properties: props2, submenu: NULL},
	{id: -1, properties: NULL, submenu: NULL}
};

proplayout_t submenu_4_3[] = {
	{id: 30, properties: props2, submenu: NULL},
	{id: 31, properties: props2, submenu: NULL},
	{id: 32, properties: props2, submenu: NULL},
	{id: 33, properties: props2, submenu: NULL},
	{id: 34, properties: props2, submenu: NULL},
	{id: 35, properties: props2, submenu: NULL},
	{id: 36, properties: props2, submenu: NULL},
	{id: 37, properties: props2, submenu: NULL},
	{id: 38, properties: props2, submenu: NULL},
	{id: 39, properties: props2, submenu: NULL},
	{id: -1, properties: NULL, submenu: NULL}
};

proplayout_t submenu_4_0[] = {
	{id: 1, properties: props2, submenu: submenu_4_1},
	{id: 2, properties: props2, submenu: submenu_4_2},
	{id: 3, properties: props2, submenu: submenu_4_3},
	{id: -1, properties: NULL, submenu: NULL}
};

proplayout_t layouts[] = {
	{id: 1, properties: props1, submenu: NULL},
	{id: 10, properties: props2, submenu: NULL},
	{id: 20, properties: props3, submenu: NULL},
	{id: 100, properties: props2, submenu: submenu_4_0},
	{id: -1, properties: NULL, submenu: NULL}
};

