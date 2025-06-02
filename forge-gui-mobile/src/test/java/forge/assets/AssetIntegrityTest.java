package forge.assets;

import com.badlogic.gdx.utils.Array;
import forge.GuiMobile;
import forge.adventure.data.EnemyData;
import forge.adventure.data.WorldData;
import forge.adventure.util.Config;
import forge.gui.GuiBase;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AssetIntegrityTest {
    @Test
    public void testAssetIntegrity() throws IOException {
        GuiBase.setInterface(new GuiMobile(Files.exists(Paths.get("./res"))?"./":"../forge-gui/"));
        GuiBase.setDeviceInfo("", "", 0, 0);

        Array<EnemyData> e = WorldData.getAllEnemies();
        SoftAssert softAssert = new SoftAssert();

        for (EnemyData enemyData : e) {
            Path atlas = Path.of(Config.instance().getCommonFilePath(enemyData.sprite));
            String imagePath = Files.readAllLines(atlas).get(0);
            File image = atlas.getParent().resolve(imagePath).toFile();
            if (!image.exists()) {
                softAssert.fail(String.format("missing resource for enemy (%s): %s", atlas.getFileName(), imagePath));
            }
        }
        softAssert.assertAll();
    }
}
