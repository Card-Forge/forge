## Part 1

### Install IntelliJ

IntelliJ comes in two versions:

- **Community Edition**: Free to download and sufficient for this project (it is not a trial version that expires).
- **Ultimate Edition**: Paid version that has support for various frameworks (which we don't need).

You can find versions for Windows, Linux, and macOS on the official website.

No additional plugins are required for this project.

  * start IntelliJ

![01-01-initial-prompt](https://user-images.githubusercontent.com/9781539/205545985-349b97e4-3262-4501-a623-d8b2eac7a34f.png)


  * select **Do not import settings**
  * click **OK**

![01-02-ui-theme-selection](https://user-images.githubusercontent.com/9781539/205546019-14de9182-1553-46a9-b041-b4d2554cf03b.png)

  * select Theme
  * click **Skip Remaining and Set Defaults**

![01-03-checkout-from-git](https://user-images.githubusercontent.com/9781539/205546065-8378c663-9d96-4180-8b04-039827a6e8a2.png)

  * select **Check out from Version Control**
  * select **Git**

![01-04-fill-in-the-form](https://user-images.githubusercontent.com/9781539/205546092-60fb070b-3696-4e0e-8fc9-208c3aaee9b6.png)

  * enter your repository/branch details
  * click **Test**

  * click **Clone**

  * -- wait ---

You will receive a prompt indicating that your checkout from version control contained a project file.  In my experience, attempting to open this file fails to do anything useful.

![01-05-do-not-open-it](https://user-images.githubusercontent.com/9781539/205546178-abef9207-a195-4a7f-96fc-a21b61b27bff.png)


  * click **No**

![01-06-click-open](https://user-images.githubusercontent.com/9781539/205546313-6c81cc08-6c34-4a14-84db-8f714ae15253.png)

  * click **Open**

![01-07-open-the-pom](https://user-images.githubusercontent.com/9781539/205546338-d89203c8-445e-47c9-91d8-c92293bde36c.png)

Browse to the folder you specified for the parent and inside the folder you specified for the git checkout.  You should see a *pom.xml* file.
  * select the *pom.xml* file
  * click **OK**

![01-08-open-as-project](https://user-images.githubusercontent.com/9781539/205546382-9678b3ba-501e-4569-86e3-9271dbdd50e3.png)


  * select **Open as Project**

At this point IntelliJ should be open with the Forge project.  However, it will take a while for IntelliJ to download all the project dependencies.  This is reflected in the bottom status bar by default.


## Part 2
  * select **File**
  * select **Project Structure...**

![02-01-project-settings](https://user-images.githubusercontent.com/9781539/205546407-d41df03f-eb8b-4727-ba11-829a2db9acfb.png)


  * select **New**
  * select **JDK**

![02-02-new-jdk](https://user-images.githubusercontent.com/9781539/205546428-cbc9fa64-6ce1-4fe1-8531-a867f8964338.png)


![02-03-jdk-homedirectory](https://user-images.githubusercontent.com/9781539/205546447-89b84446-3bc7-47f3-a6c1-167ab42de805.png)


If necessary browse to the JDK directory.
  * click **OK**

![02-04-after-jdk-setup](https://user-images.githubusercontent.com/9781539/205546459-97778249-b100-43d9-9c9a-81919f53840b.png)


  * click **OK**

  * select **Run** from the top menu
  * select **Debug...** from the drop down

![02-05-edit-configurations](https://user-images.githubusercontent.com/9781539/205546470-f9bc4146-2fd3-4e05-95ca-42bd3465c6d6.png)


  * select **Edit Configurations...**

  * click the **+** in the upper left

![02-06-application](https://user-images.githubusercontent.com/9781539/205546494-c3412aef-d4f1-4615-ac05-33449621ac5d.png)


  * select "Application"

![02-07-debug-setup](https://user-images.githubusercontent.com/9781539/205546592-6957ad7a-481f-4806-be5d-1ce2f731f13b.png)

  * set the **Name** to: Forge
  * set the **Main class** to: forge.view.Main
  * Latest IntelliJ Versions: click Modify options and check Add VM Options
  * set the **VM options** to: 
    * **(JAVA 17 and above)**
      > -Xms768m -XX:+UseParallelGC -Dsun.java2d.xrender=false --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.math=ALL-UNNAMED --add-opens java.base/java.util.concurrent=ALL-UNNAMED --add-opens java.desktop/java.awt=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.desktop/javax.swing=ALL-UNNAMED --add-opens java.desktop/java.beans=ALL-UNNAMED --add-opens java.desktop/javax.swing.border=ALL-UNNAMED -Dio.netty.tryReflectionSetAccessible=true

  * set the **Working directory** to %MODULE_WORKING_DIR%
  * set **Use classpath of module** to: forge-gui-desktop
  * click **Debug**

  * -- wait --

If all goes well, you should eventually see the Forge splash screen followed by the main UI.

### Adventure Mode debugging on Desktop

Follow the same steps to create a Run Configuration, but use forge-gui-mobile-dev instead of forge-gui-desktop as the module and directory.

  * select **Run** from the top menu
  * select **Debug...** from the drop down
  * select **Edit Configurations...**
  * click the **+** in the upper left
  * select "Application"

  * set the **Name** to: Forge
  * set the **Main class** to: forge.app.Main
  * Latest IntelliJ Versions: click Modify options and check Add VM Options
  * set the **VM options** to: 
    * **(JAVA 17 and above)**
      > -Xms768m -XX:+UseParallelGC -Dsun.java2d.xrender=false --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.math=ALL-UNNAMED --add-opens java.base/java.util.concurrent=ALL-UNNAMED --add-opens java.desktop/java.awt=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.desktop/javax.swing=ALL-UNNAMED --add-opens java.desktop/java.beans=ALL-UNNAMED --add-opens java.desktop/javax.swing.border=ALL-UNNAMED -Dio.netty.tryReflectionSetAccessible=true

  * set the **Working directory** to %MODULE_WORKING_DIR%
  * set **Use classpath of module** to: forge-gui-mobile-dev
  * click **Debug**
