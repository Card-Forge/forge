package forge.adventure.scene;

/**
 * Enum of all scenes
 */
public enum SceneType {
    StartScene(new forge.adventure.scene.StartScene()),
    NewGameScene(new forge.adventure.scene.NewGameScene()),
    SettingsScene(new forge.adventure.scene.SettingsScene()),
    GameScene(new forge.adventure.scene.GameScene()),
    DuelScene(new forge.adventure.scene.DuelScene()),
    SaveLoadScene(new forge.adventure.scene.SaveLoadScene()),
    DeckEditScene(new forge.adventure.scene.DeckEditScene()),
    TileMapScene(new forge.adventure.scene.TileMapScene()),
    RewardScene(new forge.adventure.scene.RewardScene()),
    InnScene(new forge.adventure.scene.InnScene()),
    DeckSelectScene(new forge.adventure.scene.DeckSelectScene()),
    ShopScene(new forge.adventure.scene.ShopScene());


    public final forge.adventure.scene.Scene instance;
    SceneType(forge.adventure.scene.Scene scene) {
        this.instance = scene;
    }
}
