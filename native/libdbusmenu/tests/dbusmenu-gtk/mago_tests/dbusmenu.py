from mago.test_suite.main import SingleApplicationTestSuite 
from mago.application.main import Application 

import ldtp, ooldtp, ldtputils, os.path

class DbusMenuGtkApp():
    LAUNCHER = os.path.join(os.path.dirname(__file__), "..", "dbusMenuTest")
    WINDOW   = "frmlibdbusmenu-gtktest"

    def open(self, menu_schema=''):
        ldtp.launchapp(self.LAUNCHER, [menu_schema])

    def menu_exists(self, menu=''):
        app = ooldtp.context(self.WINDOW)

        if menu == '':
            menu = "mnu1"

        try:
            component = app.getchild(menu)
        except ldtp.LdtpExecutionError:
            return False

        return True
    
    def get_submenus(self, menu=''):
        app = ooldtp.context(self.WINDOW)

        if menu == '':
            menu = "mnu1"

        component = app.getchild(menu)

        try:
            submenus = component.listsubmenus()
        except ldtp.LdtpExecutionError:
            return "" 
        
        return submenus

class DbusMenuGtkTest(SingleApplicationTestSuite):
    APPLICATION_FACTORY = DbusMenuGtkApp

    def cleanup(self):
        ldtp.waittillguinotexist(self.application.WINDOW, guiTimeOut=70)

    def teardown(self):
        ldtp.waittillguinotexist(self.application.WINDOW, guiTimeOut=70)

    def testStaticMenu(self, menu_schema, menu_item='', notexists=''):
        self.application.open(menu_schema)
        ldtp.waittillguiexist(self.application.WINDOW)

        if notexists == "True":
            if self.application.menu_exists(menu_item):
                raise AssertionError("The menu item exists")
        else:
            if not self.application.menu_exists(menu_item):
                raise AssertionError("The menu item does not exists")


    def testSubmenus(self, menu_schema, menu_item='', submenus=''):
        self.application.open(menu_schema)
        ldtp.waittillguiexist(self.application.WINDOW)

        if submenus != self.application.get_submenus(menu_item):
            raise AssertionError("The submenus are different")


         

