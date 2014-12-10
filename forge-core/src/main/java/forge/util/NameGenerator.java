/** Random name generator for Forge. */
package forge.util;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author Marc
 *
 */
public final class NameGenerator {

	private static final String[] fantasyMales = new String[] {
			"Abaet", "Abarden", "Aboloft", "Acamen", "Achard", "Ackmard", "Adeen", "Aerden", "Afflon", "Aghon", "Agnar",
			"Ahalfar", "Ahburn", "Ahdun", "Aidan", "Airen", "Airis", "Albright", "Aldaren", "Alderman", "Aldren", "Alkirk",
			"Allso", "Amerdan", "Amitel", "Anfar", "Anumi", "Anumil", "Asden", "Asdern", "Asen", "Aslan", "Atar",
			"Atgur", "Atlin", "Auchfor", "Auden", "Ault", "Ayrie", "Aysen", "Bacohl", "Badeek", "Baduk", "Balati",
			"Baradeer", "Barkydle", "Basden", "Bayde", "Beck", "Bedic", "Beeron", "Bein", "Beson", "Besur", "Besurlde",
			"Bewul", "Biedgar", "Bildon", "Biston", "Bithon", "Boal", "Boaldelr", "Bolrock", "Brakdern", "Breanon", "Bredere",
			"Bredin", "Bredock", "Breen", "Brighton", "Bristan", "Buchmeid", "Bue", "Busma", "Buthomar", "Bydern", "Caelholdt",
			"Cainon", "Calden", "Camchak", "Camilde", "Cardon", "Casden", "Cayold", "Celbahr", "Celorn", "Celthric", "Cemark",
			"Cerdern", "Cespar", "Cether", "Cevelt", "Chamon", "Chesmarn", "Chidak", "Cibrock", "Cipyar", "Ciroc", "Codern",
			"Colthan", "Connell", "Cordale", "Cos", "Cosdeer", "Cuparun", "Cusmirk", "Cydare", "Cylmar", "Cythnar", "Cyton",
			"Daburn", "Daermod", "Dak", "Dakamon", "Dakkone", "Dalburn", "Dalmarn", "Dapvhir", "Darkboon", "Darkkon", "Darko",
			"Darkspur", "Darmor", "Darpick", "Dasbeck", "Dask", "Deathmar", "Defearon", "Derik", "Derrin", "Desil", "Dessfar",
			"Dinfar", "Dismer", "Doceon", "Dochrohan", "Dokoran", "Dorn", "Dosoman", "Drakoe", "Drakone", "Drandon", "Drit",
			"Dritz", "Drophar", "Dryden", "Dryn", "Duba", "Dukran", "Duran", "Durmark", "Dusaro", "Dyfar", "Dyten",
			"Eard", "Eckard", "Efamar", "Efar", "Egmardern", "Eiridan", "Ekgamut", "Eli", "Elik", "Elson", "Elthin",
			"Enbane", "Endor", "Enidin", "Enoon", "Enro", "Erikarn", "Erim", "Eritai", "Escariet", "Espardo", "Etar",
			"Etburn", "Etdar", "Ethen", "Etmere", "Etran", "Eythil", "Faoturk", "Faowind", "Fearlock", "Fenrirr", "Fetmar",
			"Feturn", "Ficadon", "Fickfylo", "Fildon", "Firedorn", "Firiro", "Floran", "Folmard", "Fraderk", "Fronar", "Fydar",
			"Fyn", "Gafolern", "Gai", "Galain", "Galiron", "Gametris", "Gauthus", "Gemardt", "Gemedern", "Gemedes", "Gerirr",
			"Geth", "Gib", "Gibolock", "Gibolt", "Gith", "Gom", "Gosford", "Gothar", "Gothikar", "Gresforn", "Grimie",
			"Gryn", "Gundir", "Gustov", "Guthale", "Gybol", "Gybrush", "Gyin", "Halmar", "Harrenhal", "Hasten", "Hectar",
			"Hecton", "Heramon", "Hermenze", "Hermuck", "Hezak", "Hildale", "Hildar", "Hileict", "Hydale", "Hyten", "Iarmod",
			"Idon", "Ieli", "Ieserk", "Ikar", "Ilgenar", "Illilorn", "Illium", "Ingel", "Ipedorn", "Irefist", "Ironmark",
			"Isen", "Isil", "Ithric", "Jackson", "Jalil", "Jamik", "Janus", "Jayco", "Jaython", "Jesco", "Jespar",
			"Jethil", "Jex", "Jib", "Jibar", "Jin", "Juktar", "Julthor", "Jun", "Justal", "Kafar", "Kaldar",
			"Kellan", "Keran", "Kesad", "Kesmon", "Kethren", "Kib", "Kibidon", "Kiden", "Kilbas", "Kilburn", "Kildarien",
			"Kimdar", "Kinorn", "Kip", "Kirder", "Kodof", "Kolmorn", "Kyrad", "Lackus", "Lacspor", "Laderic", "Lafornon",
			"Lahorn", "Laracal", "Ledale", "Leit", "Lephar", "Lephidiles", "Lerin", "Lesphares", "Letor", "Lidorn", "Lin",
			"Liphanes", "Loban", "Lox", "Ludokrin", "Luphildern", "Lupin", "Lurd", "Macon", "Madarlon", "Mafar", "Marderdeen",
			"Mardin", "Markard", "Markdoon", "Marklin", "Mashasen", "Mathar", "Medarin", "Medin", "Mellamo", "Meowol", "Merdon",
			"Meridan", "Merkesh", "Mesah", "Mes'ard", "Mesophan", "Mesoton", "Mezo", "Michael", "Mick", "Mickal", "Migorn",
			"Milo", "Miphates", "Mi'talrythin", "Mitar", "Modric", "Modum", "Mudon", "Mufar", "Mujarin", "Mylo", "Mythik",
			"Mythil", "Nadeer", "Nalfar", "Namorn", "Naphates", "Neowyld", "Nidale", "Nikpal", "Nikrolin", "Niktohal", "Niro",
			"Noford", "Nothar", "Nuthor", "Nuwolf", "Nydale", "Nythil", "O'tho", "Ocarin", "Occelot", "Occhi", "Odaren",
			"Odeir", "Ohethlic", "Okar", "Omaniron", "Omarn", "Orin", "Ospar", "Othelen", "Oxbaren", "Padan", "Palid",
			"Papur", "Peitar", "Pelphides", "Pender", "Pendus", "Perder", "Perol", "Phairdon", "Phemedes", "Phexides", "Phoenix",
			"Picon", "Pictal", "Picumar", "Pildoor", "Pixdale", "Ponith", "Poran", "Poscidion", "Prothalon", "Puthor", "Pyder",
			"Qeisan", "Qidan", "Quiad", "Quid", "Quiss", "Qupar", "Qysan", "Radag'mal", "Randar", "Raysdan", "Rayth",
			"Reaper", "Resboron", "Reth", "Rethik", "Rhithik", "Rhithin", "Rhysling", "Riandur", "Rikar", "Rismak", "Riss",
			"Ritic", "Rogeir", "Rogist", "Rogoth", "Rophan", "Rulrindale", "Rydan", "Ryfar", "Ryfar", "Ryodan", "Rysdan",
			"Rythen", "Rythern", "Sabal", "Sadareen", "Safilix", "Samon", "Samot", "Sasic", "Scoth", "Scythe", "Secor",
			"Sed", "Sedar", "Senick", "Senthyril", "Serin", "Sermak", "Seryth", "Sesmidat", "Seth", "Setlo", "Shade",
			"Shadowbane", "Shane", "Shard", "Shardo", "Shillen", "Silco", "Sildo", "Sil'forrin", "Silpal", "Sithik", "Soderman",
			"Sothale", "Staph", "Stenwulf", "Steven", "Suktor", "Suth", "Sutlin", "Syr", "Syth", "Sythril", "Talberon",
			"Telpur", "Temil", "Temilfist", "Tempist", "Teslanar", "Tespar", "Tessino", "Tethran", "Thiltran", "Tholan", "Tibers",
			"Tibolt", "Ticharol", "Tilner", "Tithan", "Tobale", "Tol'Solie", "Tolle", "Tolsar", "Toma", "Tothale", "Tousba",
			"Towerlock", "Tuk", "Tuscanar", "Tusdar", "Tyden", "Uerthe", "Ugmar", "Uhrd", "Undin", "Updar", "Uther",
			"Vaccon", "Vacone", "Valkeri", "Valynard", "Vectomon", "Veldahar", "Vespar", "Vethelot", "Victor", "Vider", "Vigoth",
			"Vilan", "Vildar", "Vinald", "Vinkolt", "Virde", "Voltain", "Volux", "Voudim", "Vythethi", "Wak'dern", "Walkar",
			"Wanar", "Wekmar", "Werymn", "Weshin", "William", "Willican", "Wilte", "Wiltmar", "Wishane", "Witfar", "Wrathran",
			"Wraythe", "Wuthmon", "Wyder", "Wyeth", "Wyvorn", "Xander", "Xavier", "Xenil", "Xex", "Xithyl", "Xuio",
			"Xynx", "Y'reth", "Yabaro", "Yepal", "Yesirn", "Yssik", "Yssith", "Zak", "Zakarn", "Zecane", "Zeke",
			"Zerin", "Zessfar", "Zidar", "Zigmal", "Zile", "Zilocke", "Zio", "Zoru", "Zotar", "Zutar", "Zyten"
	};

	private static final String[] fantasyFemales = new String[] {
			"Acele", "Acholate", "Ada", "Adiannon", "Adorra", "Ahanna", "Akara", "Akassa", "Akia", "Amaerilde", "Amara",
			"Amarisa", "Amarizi", "Ana", "Andonna", "Ani", "Annalyn", "Archane", "Ariannona", "Arina", "Arryn", "Asada",
			"Awnia", "Ayne", "Basete", "Bathelie", "Bethe", "Brana", "Brianan", "Bridonna", "Brynhilde", "Calene", "Calina",
			"Celestine", "Celoa", "Cephenrene", "Chani", "Chivahle", "Chrystyne", "Corda", "Cyelena", "Dalavesta", "Desini", "Dylena",
			"Ebatryne", "Ecematare", "Efari", "Enaldie", "Enoka", "Enoona", "Errinaya", "Fayne", "Frederika", "Frida", "Gene",
			"Gessane", "Gronalyn", "Gvene", "Gwethana", "Halete", "Helenia", "Hildandi", "Hyza", "Idona", "Ikini", "Ilene",
			"Illia", "Iona", "Jessika", "Jezzine", "Justalyne", "Kassina", "Kilayox", "Kilia", "Kilyne", "Kressara", "Laela",
			"Laenaya", "Lelani", "Lenala", "Linovahle", "Linyah", "Lloyanda", "Lolinda", "Lyna", "Lynessa", "Mehande", "Melisande",
			"Midiga", "Mirayam", "Mylene", "Nachaloa", "Naria", "Narisa", "Nelenna", "Niraya", "Nymira", "Ochala", "Olivia",
			"Onathe", "Ondola", "Orwyne", "Parthinia", "Pascheine", "Pela", "Peri'el", "Pharysene", "Philadona", "Prisane", "Prysala",
			"Pythe", "Q'ara", "Q'pala", "Quasee", "Rhyanon", "Rivatha", "Ryiah", "Sanala", "Sathe", "Senira", "Sennetta",
			"Sepherene", "Serane", "Sevestra", "Sidara", "Sidathe", "Sina", "Sunete", "Synestra", "Sythini", "Szene", "Tabika",
			"Tabithi", "Tajule", "Tamare", "Teresse", "Tolida", "Tonica", "Treka", "Tressa", "Trinsa", "Tryane", "Tybressa",
			"Tycane", "Tysinni", "Undaria", "Uneste", "Urda", "Usara", "Useli", "Ussesa", "Venessa", "Veseere", "Voladea",
			"Vysarane", "Vythica", "Wanera", "Welisarne", "Wellisa", "Wesolyne", "Wyeta", "Yilvoxe", "Ysane", "Yve", "Yviene",
			"Yvonnette", "Yysara", "Zana", "Zathe", "Zecele", "Zenobia", "Zephale", "Zephere", "Zerma", "Zestia", "Zilka",
			"Zoura", "Zrye", "Zyneste", "Zynoa"
	};

	private static final String[] genericMales = new String[] {
			"Jacob", "Michael", "Joshua", "Matthew", "Daniel", "Christopher", "Andrew", "Ethan", "Joseph", "William",
			"Anthony", "David", "Alexander", "Nicholas", "Ryan", "Tyler", "James", "John", "Jonathan", "Noah",
			"Brandon", "Christian", "Dylan", "Samuel", "Benjamin", "Nathan", "Zachary", "Logan", "Justin", "Gabriel",
			"Jose", "Austin", "Kevin", "Elijah", "Caleb", "Robert", "Thomas", "Jordan", "Cameron", "Jack",
			"Hunter", "Jackson", "Angel", "Isaiah", "Evan", "Isaac", "Mason", "Luke", "Jason", "Jayden",
			"Gavin", "Aaron", "Connor", "Aiden", "Aidan", "Kyle", "Juan", "Charles", "Luis", "Adam",
			"Lucas", "Brian", "Eric", "Adrian", "Nathaniel", "Sean", "Alex", "Carlos", "Ian", "Bryan",
			"Owen", "Jesus", "Landon", "Julian", "Chase", "Cole", "Diego", "Jeremiah", "Steven", "Sebastian",
			"Xavier", "Timothy", "Carter", "Wyatt", "Brayden", "Blake", "Hayden", "Devin", "Cody", "Richard",
			"Seth", "Dominic", "Jaden", "Antonio", "Miguel", "Liam", "Patrick", "Carson", "Jesse", "Tristan",
			"Alejandro", "Henry", "Victor", "Trevor", "Bryce", "Jake", "Riley", "Colin", "Jared", "Jeremy",
			"Mark", "Caden", "Garrett", "Parker", "Marcus", "Vincent", "Kaleb", "Kaden", "Brady", "Colton",
			"Kenneth", "Joel", "Oscar", "Josiah", "Jorge", "Cooper", "Ashton", "Tanner", "Eduardo", "Paul",
			"Edward", "Ivan", "Preston", "Maxwell", "Alan", "Levi", "Stephen", "Grant", "Nicolas", "Omar",
			"Dakota", "Alexis", "George", "Collin", "Eli", "Spencer", "Gage", "Max", "Cristian", "Ricardo",
			"Derek", "Micah", "Brody", "Francisco", "Nolan", "Ayden", "Dalton", "Shane", "Peter", "Damian",
			"Jeffrey", "Brendan", "Travis", "Fernando", "Peyton", "Conner", "Andres", "Javier", "Giovanni", "Shawn",
			"Braden", "Jonah", "Bradley", "Cesar", "Emmanuel", "Manuel", "Edgar", "Mario", "Erik", "Edwin",
			"Johnathan", "Devon", "Erick", "Wesley", "Oliver", "Trenton", "Hector", "Malachi", "Jalen", "Raymond",
			"Gregory", "Abraham", "Elias", "Leonardo", "Sergio", "Donovan", "Colby", "Marco", "Bryson", "Martin",
			"Zoura", "Zrye", "Zyneste", "Zynoa", "Aaron", "Abraham", "Adam",
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

	private static final String[] genericFemales = new String[] {
			"Emily", "Madison", "Emma", "Olivia", "Hannah", "Abigail", "Isabella", "Samantha", "Elizabeth", "Ashley",
			"Alexis", "Sarah", "Sophia", "Alyssa", "Grace", "Ava", "Taylor", "Brianna", "Lauren", "Chloe",
			"Natalie", "Kayla", "Jessica", "Anna", "Victoria", "Mia", "Hailey", "Sydney", "Jasmine", "Julia",
			"Morgan", "Destiny", "Rachel", "Ella", "Kaitlyn", "Megan", "Katherine", "Savannah", "Jennifer", "Alexandra",
			"Allison", "Haley", "Maria", "Kaylee", "Lily", "Makayla", "Brooke", "Mackenzie", "Nicole", "Addison",
			"Stephanie", "Lillian", "Andrea", "Zoe", "Faith", "Kimberly", "Madeline", "Alexa", "Katelyn", "Gabriella",
			"Gabrielle", "Trinity", "Amanda", "Kylie", "Mary", "Paige", "Riley", "Leah", "Jenna", "Sara",
			"Rebecca", "Michelle", "Sofia", "Vanessa", "Jordan", "Angelina", "Caroline", "Avery", "Audrey", "Evelyn",
			"Maya", "Claire", "Autumn", "Jocelyn", "Ariana", "Nevaeh", "Arianna", "Jada", "Bailey", "Brooklyn",
			"Aaliyah", "Amber", "Isabel", "Mariah", "Danielle", "Melanie", "Sierra", "Erin", "Molly", "Amelia",
			"Isabelle", "Madelyn", "Melissa", "Jacqueline", "Marissa", "Shelby", "Angela", "Leslie", "Katie", "Jade",
			"Catherine", "Diana", "Aubrey", "Mya", "Amy", "Briana", "Sophie", "Gabriela", "Breanna", "Gianna",
			"Kennedy", "Gracie", "Peyton", "Adriana", "Christina", "Courtney", "Daniela", "Lydia", "Kathryn", "Valeria",
			"Layla", "Alexandria", "Natalia", "Angel", "Laura", "Charlotte", "Margaret", "Cheyenne", "Mikayla", "Miranda",
			"Naomi", "Kelsey", "Payton", "Ana", "Alicia", "Jillian", "Daisy", "Mckenzie", "Ashlyn", "Sabrina",
			"Caitlin", "Summer", "Ruby", "Rylee", "Valerie", "Skylar", "Lindsey", "Kelly", "Genesis", "Zoey",
			"Eva", "Sadie", "Alexia", "Cassidy", "Kylee", "Kendall", "Jordyn", "Kate", "Jayla", "Karen",
			"Tiffany", "Cassandra", "Juliana", "Reagan", "Caitlyn", "Giselle", "Serenity", "Alondra", "Lucy", "Bianca",
			"Kiara", "Crystal", "Erica", "Angelica", "Hope", "Chelsea", "Alana", "Liliana", "Brittany", "Camila",
			"Makenzie", "Lilly", "Veronica", "Abby", "Jazmin", "Adrianna", "Delaney", "Karina", "Ellie", "Jasmin",
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
            "Victoria", "Violet", "Vivian", "Wendy", "Winnie", "Yvonne"
	};

	private static final String[] monikers = new String[] {
			// funny and insulting (great for more brutish characters)
			"Numbfoot", "Knockerface", "Wipedolt", "Doofcorn", "Headwipe", "Twerpknock", "Muckfumble",
			"Knucklegoof", "Bumpbumbeef", "Bumbleknuckle", "Headpuff", "Facewimp", "Sneezeankle", "Twerpthimble",
			"Doofgoof", "Snarkbumble", "Wadwipe", "Puffsnark", "Airdoof", "Headcorn", "Footmunch",
			"Goofgrumble", "Twitmeat", "Beefankle", "Clodbumble", "Loafhead", "Munchwipe", "Meatgoof",
			"Pinanklepuff", "Cheeseball", "Headknuckle", "Beefmeat", "Meatsnark", "Doofball", "Pufflump",
			"Footgoof", "Diptwerp", "Knockskull", "Lumpface", "Twerplump", "Bonebump", "Beeffoot",
			"Skullpin", "Twitlunk", "Snarkboneknuckle", "Ramblemouth", "Twitskull", "Bonedork",
			"Facelump", "Foottwit", "Wadankle", "Knockermunch", "Numbsnark", "Nitnumb", "Twitwad",
			"Loafknock", "Lumpsneeze", "Bumgrumble", "Boneface", "Dorkbum", "Beefclotclot", "Fingermunch",
			"Knucklepuff", "Flatface", "Wimpair", "Wimpball", "Snarkpuff",
			// descriptive of deeds or appearance
			"the Brave", "Bloodletter", "One-Eyed", "the Black", "Scarshadow", "Shadowwalker", "Fleetfoot",
			"the Short", "Pureheart", "Shadowheart", "Voidbringer", "Warbringer", "the White", "Spellslinger",
			"Spellweaver", "the Hidden", "the Stout", "the Wary", "Storm Caller", "Dawnseeker", "the Aloof",
			"Bloodfist", "Spellbreaker", "Warcaller", "the Untouchable", "the Vain", "Surehit", "Fireheart",
			"Goblinbane", "Boarjaw", "Swordhawk", "Screamforge", "Sunsforge", "Oakenhelm", "Ironrunner",
			"Coreshaker", "Forgewulf", "Sheepspear", "Elvenworm", "Lipswalker", "Sealight", "the Rotten"
	};

	private static List<String> usedMonikers = new ArrayList<String>();
	private static List<String> usedNames;
    private static String[] sourceList;

	/** Generates a single name that doesn't match the specified name. 
	 * 
	 * @param gender        String specifying the desired gender. Recognises "Male", "Female" and "Any".
	 * @param type          String specifying the desired type. Recognises "Fantasy", "Generic" and "Any".
	 * @param excludeNames  A list of names already being used.
	 * @return              Returns the generated name string.
	 */
	public static String getRandomName(final String gender, final String type, final List<String> excludeNames) {
		usedNames = excludeNames;
		String name = "";

        Random seed = MyRandom.getRandom();
        boolean useMoniker = false;

        switch (type + gender) {
        case "GenericMale":		{ sourceList = genericMales;	useMoniker = seed.nextFloat() <= 0.03f;		break;}
        case "GenericFemale":	{ sourceList = genericFemales;	useMoniker = seed.nextFloat() <= 0.02f;		break;}
        case "FantasyMale":		{ sourceList = fantasyMales;	useMoniker = seed.nextFloat() <= 0.10f;		break;}
        case "FantasyFemale":	{ sourceList = fantasyFemales;	useMoniker = seed.nextFloat() <= 0.08f;		break;}

        case "AnyMale":
        	sourceList = getCombinedLists(genericMales, fantasyMales);
        	useMoniker = seed.nextFloat() <= 0.06f;
        	break;
        
        case "AnyFemale":
        	sourceList = getCombinedLists(genericFemales, fantasyFemales);
        	useMoniker = seed.nextFloat() <= 0.025f;
        	break;
        
        case "GenericAny":
        	sourceList = getCombinedLists(genericMales, genericFemales);
        	useMoniker = seed.nextFloat() <= 0.015f;
        	break;

        case "FantasyAny":
        	sourceList = getCombinedLists(fantasyMales, fantasyFemales);
        	useMoniker = seed.nextFloat() <= 0.06f;
        	break;

        default:
        	List<String> all = new ArrayList<String>(
        			genericMales.length + fantasyMales.length + genericFemales.length + fantasyFemales.length);
        	Collections.addAll(all, genericMales);
        	Collections.addAll(all, fantasyMales);
        	Collections.addAll(all, genericFemales);
        	Collections.addAll(all, fantasyFemales);
        	sourceList = all.toArray(new String[all.size()]);
        	useMoniker = seed.nextFloat() <= 0.04f;
        	break;
        }

        do {
        	name = sourceList[seed.nextInt(sourceList.length)];
        } while (excludeNames.contains(name));

        usedNames.add(name); // add base name to used names list

        if (useMoniker) {
        	String moniker = "";
        	do {
        		moniker = monikers[seed.nextInt(monikers.length)];
        	} while (usedMonikers.contains(moniker));

    		usedMonikers.add(moniker);
    		name += " " + moniker;
        }

		return name;
	}

	/** Generates a specified number of random names. */
	public static List<String> getRandomNames(final int generateAmount, final List<String> excludeNames) {
		usedNames = excludeNames;
		final List<String> names = new ArrayList<String>(generateAmount);
        for (int i = 0; i < generateAmount; i++) {
        	getRandomName("Any", "Any", usedNames);
        }
        return names;
	}

	/** Generates a single name that doesn't match any names in the supplied list. */
	public static String getRandomName(final String gender, final String type, final String notNamed) {
		List<String> exclude = new ArrayList<String>(1);
		exclude.add(notNamed);
		return getRandomName(gender, type, exclude);
	}

	private final static String[] getCombinedLists(String[] listOne, String[] listTwo) {
		String[] joined = ArrayUtils.addAll(listOne,listTwo);
		return joined;
	}
}
