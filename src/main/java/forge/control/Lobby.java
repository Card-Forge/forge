package forge.control;

import java.util.Random;

import forge.game.player.LobbyPlayer;
import forge.game.player.PlayerType;
import forge.gui.toolbox.FSkin;
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

    /**
     * TODO: Write javadoc for this method.
     * @param human
     * @return
     */
    public LobbyPlayer findLocalPlayer(PlayerType type, String name) {

        return new LobbyPlayer(type, name);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param human
     * @return
     */
    public LobbyPlayer findLocalPlayer(PlayerType type) {
        if (type == PlayerType.HUMAN) {
            return new LobbyPlayer(type, "Human"); // need to get name!
        }

        LobbyPlayer player = findLocalPlayer(type, getRandomName());
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
        return new LobbyPlayer(PlayerType.HUMAN, "Human"); // need to get name!
    }


}
