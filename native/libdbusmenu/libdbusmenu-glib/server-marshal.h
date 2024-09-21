
#ifndef ___dbusmenu_server_marshal_MARSHAL_H__
#define ___dbusmenu_server_marshal_MARSHAL_H__

#include	<glib-object.h>

G_BEGIN_DECLS

/* VOID:INT,STRING,VARIANT (./server-marshal.list:1) */
extern void _dbusmenu_server_marshal_VOID__INT_STRING_VARIANT (GClosure     *closure,
                                                               GValue       *return_value,
                                                               guint         n_param_values,
                                                               const GValue *param_values,
                                                               gpointer      invocation_hint,
                                                               gpointer      marshal_data);

/* VOID:UINT,INT (./server-marshal.list:2) */
extern void _dbusmenu_server_marshal_VOID__UINT_INT (GClosure     *closure,
                                                     GValue       *return_value,
                                                     guint         n_param_values,
                                                     const GValue *param_values,
                                                     gpointer      invocation_hint,
                                                     gpointer      marshal_data);

/* VOID:INT,UINT (./server-marshal.list:3) */
extern void _dbusmenu_server_marshal_VOID__INT_UINT (GClosure     *closure,
                                                     GValue       *return_value,
                                                     guint         n_param_values,
                                                     const GValue *param_values,
                                                     gpointer      invocation_hint,
                                                     gpointer      marshal_data);

G_END_DECLS

#endif /* ___dbusmenu_server_marshal_MARSHAL_H__ */

