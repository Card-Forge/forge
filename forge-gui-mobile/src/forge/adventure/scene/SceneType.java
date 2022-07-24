package forge.adventure.scene;

/**
 * Enum of all scenes
 */
public enum SceneType {
    StartScene(new StartScene()),
    NewGameScene(new NewGameScene()),
    SettingsScene(new SettingsScene()),
    GameScene(new GameScene()),
    DuelScene(new DuelScene()),
    SaveLoadScene(new SaveLoadScene()),
    DeckEditScene(new DeckEditScene()),
    TileMapScene(new TileMapScene()),
    RewardScene(new RewardScene()),
    InnScene(new InnScene()),
    DeckSelectScene(new DeckSelectScene()),
    ShopScene(new ShopScene()),
    PlayerStatisticScene(new PlayerStatisticScene()),
    InventoryScene(new InventoryScene()),
    SpellSmithScene(new SpellSmithScene());

    public final Scene instance;
    SceneType(Scene scene) {
        this.instance = scene;
    }
}
