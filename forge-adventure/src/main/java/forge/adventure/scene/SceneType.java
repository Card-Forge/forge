package forge.adventure.scene;


public enum SceneType {
    StartScene(new forge.adventure.scene.StartScene()),
    NewGameScene(new forge.adventure.scene.NewGameScene()),
    SettingsScene(new forge.adventure.scene.SettingsScene()),
    GameScene(new forge.adventure.scene.GameScene()),
    DuelScene(new forge.adventure.scene.DuelScene())
    ;
    public final forge.adventure.scene.Scene instance;
    SceneType(forge.adventure.scene.Scene scene) {
        this.instance = scene;
    }
}
