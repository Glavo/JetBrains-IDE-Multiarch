
#ifndef ___dbusmenu_menuitem_marshal_MARSHAL_H__
#define ___dbusmenu_menuitem_marshal_MARSHAL_H__

#include	<glib-object.h>

G_BEGIN_DECLS

/* VOID:STRING,VARIANT (./menuitem-marshal.list:1) */
extern void _dbusmenu_menuitem_marshal_VOID__STRING_VARIANT (GClosure     *closure,
                                                             GValue       *return_value,
                                                             guint         n_param_values,
                                                             const GValue *param_values,
                                                             gpointer      invocation_hint,
                                                             gpointer      marshal_data);

/* VOID:OBJECT,UINT,UINT (./menuitem-marshal.list:2) */
extern void _dbusmenu_menuitem_marshal_VOID__OBJECT_UINT_UINT (GClosure     *closure,
                                                               GValue       *return_value,
                                                               guint         n_param_values,
                                                               const GValue *param_values,
                                                               gpointer      invocation_hint,
                                                               gpointer      marshal_data);

/* VOID:OBJECT,UINT (./menuitem-marshal.list:3) */
extern void _dbusmenu_menuitem_marshal_VOID__OBJECT_UINT (GClosure     *closure,
                                                          GValue       *return_value,
                                                          guint         n_param_values,
                                                          const GValue *param_values,
                                                          gpointer      invocation_hint,
                                                          gpointer      marshal_data);

/* VOID:OBJECT (./menuitem-marshal.list:4) */
#define _dbusmenu_menuitem_marshal_VOID__OBJECT	g_cclosure_marshal_VOID__OBJECT

/* VOID:VOID (./menuitem-marshal.list:5) */
#define _dbusmenu_menuitem_marshal_VOID__VOID	g_cclosure_marshal_VOID__VOID

/* VOID:UINT (./menuitem-marshal.list:6) */
#define _dbusmenu_menuitem_marshal_VOID__UINT	g_cclosure_marshal_VOID__UINT

/* BOOLEAN:STRING,VARIANT,UINT (./menuitem-marshal.list:7) */
extern void _dbusmenu_menuitem_marshal_BOOLEAN__STRING_VARIANT_UINT (GClosure     *closure,
                                                                     GValue       *return_value,
                                                                     guint         n_param_values,
                                                                     const GValue *param_values,
                                                                     gpointer      invocation_hint,
                                                                     gpointer      marshal_data);

G_END_DECLS

#endif /* ___dbusmenu_menuitem_marshal_MARSHAL_H__ */

