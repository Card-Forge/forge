package forge.control;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import forge.game.player.LobbyPlayer;
import forge.game.player.LobbyPlayerAi;
import forge.game.player.LobbyPlayerHuman;
import forge.game.player.LobbyPlayerRemote;
import forge.gui.toolbox.FSkin;
import forge.net.client.INetClient;
import forge.util.MyRandom;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class Lobby {


    private final String[] opponentNames = new String[] {
            "Abigail", "Ada", "Adeline", "Adriana", "Agatha", "Agnes", "Aileen", "Alba", "Alcyon",
            "Alethea", "Alice", "Alicia", "Alison", "Amanda", "Amelia", "Amy", "Andrea", "Angelina",
            "Anita", "Ann", "Annabel", "Anne", "Audrey", "Barbara", "Belinda", "Bernice", "Bertha",
            "Bonnie", "Brenda", "Bridget", "Bunny", "Carmen", "Carol", "Catherine", "Cheryl",
            "Christine", "Cinderalla", "Claire", "Clarice", "Claudia", "Constance", "Cora",
            "Corinne", "Cnythia", "Daisy", "Daphne", "Dawn", "Deborah", "Diana", "Dolly", "Dora",
            "Doreen", "Doris", "Dorothy", "Eileen", "Elaine", "Elizabeth", "Emily", "Emma", "Ethel",
            "Evelyn", "Fiona", "Florence", "Frances", "Geraldine", "Gertrude", "Gladys", "Gloria",
            "Grace", "Greta", "Harriet", "Hazel", "Helen", "Hilda", "Ida", "Ingrid", "Irene",
            "Isabel", "Jacinta", "Jackie", "Jane", "Janet", "Janice", "Jennifer", "Jessie", "Joan",
            "Jocelyn", "Josephine", "Joyce", "Judith", "Julia", "Juliana", "Karina", "Kathleen",
            "Laura", "Lilian", "Lily", "Linda", "Lisa", "Lilita", "Lora", "Lorna", "Lucy", "Lydia",
            "Mabel", "Madeline", "Maggie", "Maria", "Mariam", "Marilyn", "Mary", "Matilda", "Mavis",
            "Melanie", "Melinda", "Melody", "Michelle", "Mildred", "Molly", "Mona", "Monica",
            "Nancy", "Nora", "Norma", "Olga", "Pamela", "Patricia", "Paula", "Pauline", "Pearl",
            "Peggy", "Penny", "Phoebe", "Phyllis", "Polly", "Priscilla", "Rachel", "Rebecca",
            "Rita", "Rosa", "Rosalind", "Rose", "Rosemary", "Rowena", "Ruby", "Sally", "Samantha",
            "Sarah", "Selina", "Sharon", "Sheila", "Shirley", "Sonya", "Stella", "Sue", "Susan",
            "Sylvia", "Tina", "Tracy", "Ursula", "Valentine", "Valerie", "Vanessa", "Veronica",
            "Victoria", "Violet", "Vivian", "Wendy", "Winnie", "Yvonne", "Aaron", "Abraham", "Adam",
            "Adrain", "Alain", "Alan", "Alban", "Albert", "Alec", "Alexander", "Alfonso", "Alfred",
            "Allan", "Allen", "Alonso", "Aloysius", "Alphonso", "Alvin", "Andrew", "Andy", "Amadeus",
            "Amselm", "Anthony", "Arnold", "Augusta", "Austin", "Barnaby", "Benedict", "Benjamin",
            "Bertie", "Bertram", "Bill", "Bob", "Boris", "Brady", "Brian", "Bruce", "Burt", "Byron",
            "Calvin", "Carl", "Carter", "Casey", "Cecil", "Charles", "Christian", "Christopher",
            "Clarence", "Clement", "Colin", "Conan", "Dalton", "Damian", "Daniel", "David", "Denis",
            "Derek", "Desmond", "Dick", "Dominic", "Donald", "Douglas", "Duncan", "Edmund",
            "Edward", "Ellen", "Elton", "Elvis", "Eric", "Eugene", "Felix", "Francis", "Frank",
            "Frederick", "Gary", "Geoffrey", "George", "Gerald", "Gerry", "Gordon", "Hamish",
            "Hardy", "Harold", "Harry", "Henry", "Herbert", "Ignatius", "Jack", "James", "Jeffrey",
            "Jim", "Joe", "John", "Joseph", "Karl", "Keith", "Kenneth", "Kevin", "Larry", "Lawrence",
            "Leonard", "Lionel", "Louis", "Lucas", "Malcolm", "Mark", "Martin", "Mathew", "Maurice",
            "Max", "Melvin", "Michael", "Milton", "Morgan", "Morris", "Murphy", "Neville",
            "Nicholas", "Noel", "Norman", "Oliver", "Oscar", "Patrick", "Paul", "Perkin", "Peter",
            "Philip", "Ralph", "Randy", "Raymond", "Richard", "Ricky", "Robert", "Robin", "Rodney",
            "Roger", "Roland", "Ronald", "Roy", "Sam", "Sebastian", "Simon", "Stanley", "Stephen",
            "Stuart", "Terence", "Thomas", "Tim", "Tom", "Tony", "Victor", "Vincent", "Wallace",
            "Walter", "Wilfred", "William", "Winston"
    };

    private Map<String, LobbyPlayerRemote> remotePlayers = new ConcurrentHashMap<String, LobbyPlayerRemote>();
    private final LobbyPlayerHuman guiPlayer = new LobbyPlayerHuman("Human");
    private final LobbyPlayerAi system = new LobbyPlayerAi("System");

    public final LobbyPlayerHuman getGuiPlayer() {
        return guiPlayer;
    }

    public final LobbyPlayer getAiPlayer() { return getAiPlayer(getRandomName()); }
    public final LobbyPlayer getAiPlayer(String name) {
        LobbyPlayer player = new LobbyPlayerAi(name);
        player.setAvatarIndex(MyRandom.getRandom().nextInt(FSkin.getAvatars().size()));
        return player;
    }


    /**
     * TODO: Write javadoc for this method.
     * @param nextInt
     * @return
     */
    private String getRandomName() {
        Random my = MyRandom.getRandom();
        return opponentNames[my.nextInt(opponentNames.length)];

    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public LobbyPlayer getQuestPlayer() {
        return guiPlayer;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param name
     * @return
     */
    public synchronized LobbyPlayer findOrCreateRemotePlayer(String name, INetClient client) {
        if (remotePlayers.containsKey(name))
            return remotePlayers.get(name);

        LobbyPlayerRemote res = new LobbyPlayerRemote(name, client);
        speak(ChatArea.Room, system, res.getName()  + " has joined the server.");
        // have to load avatar from remote user's preferences here
        remotePlayers.put(name, res);
        
        return res;
    }

    public void disconnectPlayer(LobbyPlayer player) {
        // Should set up a timer here to discard player and all of his games after 20 minutes of being offline
    }


    public void speak(ChatArea room, LobbyPlayer player, String message) {
        getGuiPlayer().hear(player, message);
        for(LobbyPlayer remote : remotePlayers.values()) {
            remote.hear(player, message);
        }
    }
}
