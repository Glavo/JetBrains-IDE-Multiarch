
#ifndef ___dbusmenu_client_marshal_MARSHAL_H__
#define ___dbusmenu_client_marshal_MARSHAL_H__

#include	<glib-object.h>

G_BEGIN_DECLS

/* VOID:OBJECT,UINT (./client-marshal.list:1) */
extern void _dbusmenu_client_marshal_VOID__OBJECT_UINT (GClosure     *closure,
                                                        GValue       *return_value,
                                                        guint         n_param_values,
                                                        const GValue *param_values,
                                                        gpointer      invocation_hint,
                                                        gpointer      marshal_data);

/* VOID:OBJECT,STRING,VARIANT,UINT,POINTER (./client-marshal.list:2) */
extern void _dbusmenu_client_marshal_VOID__OBJECT_STRING_VARIANT_UINT_POINTER (GClosure     *closure,
                                                                               GValue       *return_value,
                                                                               guint         n_param_values,
                                                                               const GValue *param_values,
                                                                               gpointer      invocation_hint,
                                                                               gpointer      marshal_data);

/* VOID:ENUM (./client-marshal.list:3) */
#define _dbusmenu_client_marshal_VOID__ENUM	g_cclosure_marshal_VOID__ENUM

/* VOID:POINTER (./client-marshal.list:4) */
#define _dbusmenu_client_marshal_VOID__POINTER	g_cclosure_marshal_VOID__POINTER

G_END_DECLS

#endif /* ___dbusmenu_client_marshal_MARSHAL_H__ */

