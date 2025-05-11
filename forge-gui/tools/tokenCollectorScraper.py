import os
import requests
from time import sleep

# Function to parse files and extract relevant data
def process_files(directory):
    token_data = []

    skipahead = True

    for filename in os.listdir(directory):
        filepath = os.path.join(directory, filename)
        if filename == "Commander 2014.txt":
            skipahead = False

        if skipahead:
            continue

        if not os.path.isfile(filepath):
            continue

        print(filepath)

        scryfall_code = None
        newlines = []
        tokens = []
        with open(filepath, "r", encoding='utf-8') as file:
            lines = file.readlines()
            tokens_activated = False

            for line in lines:
                if not tokens_activated:
                    newlines.append(line)
                line = line.strip()
                if line.startswith("ScryfallCode="):
                    scryfall_code = line.split("=")[1]
                elif line.startswith("[tokens]"):
                    tokens_activated = True
                    continue
                elif line and tokens_activated:
                    tokens.append(line)

        if not scryfall_code:
            print(f"No ScryfallCode found in {filename}.")
            continue

        # Call the cross-reference function here if needed
        if len(tokens) == 0:
            continue

        tokenCode = "T" + scryfall_code
        api_data = fetch_scryfall_data(tokenCode)
        if not api_data:
            print(f"No data found for {scryfall_code}.")
            continue

        matches = cross_reference(tokens, api_data)
        if not matches:
            print(f"No matches found for {scryfall_code}.")
            continue

        try:
            matches.sort(key=lambda x: int(x[0]))
        except:
            # Because yknow collector numbers can be strings
            matches.sort(key=lambda x: x[0])

        with open(filepath, "w", encoding='utf-8') as file:
            for line in newlines:
                file.write(line)

            for number, filename, artist in matches:
                file.write(f"{number} {filename} {artist}\n")

        print("Updated file:", filepath)
        sleep(0.2)  # Avoid hitting the API too hard

    return token_data

# Function to fetch data from Scryfall API
def fetch_scryfall_data(code):
    url = f"https://api.scryfall.com/cards/search?q=e:{code.lower()}"
    response = requests.get(url)

    if response.status_code == 200:
        return response.json()

    print(f"Error fetching Scryfall data for {code}")
    return None

# Function to cross-reference tokens with API results
# {"name":"Beast","type_line":"Token Creature — Beast","oracle_text":"","power":"3","toughness":"3","colors":["G"],"keywords":[]}
def cross_reference(tokens, api_data):
    matched_tokens = []

    if "data" in api_data:
        card_data = api_data["data"]

        simplified_tokens = {}
        found_collector_numbers = set()
        for card in card_data:
            built_filename, exact_name, artist = predict_filename(card)
            simplified_tokens[built_filename] = (card["collector_number"], artist)
            simplified_tokens[exact_name] = (card["collector_number"], artist)

        for token in tokens:
            if token in simplified_tokens:
                collector_number = simplified_tokens[token][0]
                artist = simplified_tokens[token][1]
                matched_tokens.append((collector_number, token, artist))
                found_collector_numbers.add(collector_number)
                continue

            # I have duplicates in the data. How do I make sure to skip them doing this method?
            for key in simplified_tokens:
                collector_number = simplified_tokens[key][0]
                if token.startswith(key):
                    try:
                        while collector_number in found_collector_numbers:
                            collector_number = str(int(collector_number)+1)
                    except:
                        print("UNABLE TO EXPAND COLLECTOR NUMBER", collector_number)

                    artist = simplified_tokens[key][1]
                    matched_tokens.append((collector_number, token, artist))
                    found_collector_numbers.add(collector_number)
                    break
            else:
                # I couldn't find a match. Fill in a partial match to be manually fixed later.
                matched_tokens.append(("0", token, "@Unknown"))

    return matched_tokens


def predict_filename(card):
    filename_builder = []


    if "colors" in card and len(card["colors"]) > 1:
        color_order = {"W": 0, "U": 1, "B": 2, "R": 3, "G": 4}
        colors = card["colors"]
        colors.sort(key=lambda color: color_order[color])
        filename_builder.append("".join(colors).lower())
    else:
        filename_builder.append('c' if ("colors" not in card or card["colors"] == []) else "".join(card["colors"]).lower())

    if "power" in card:
        filename_builder.append(card["power"].replace("*", "x"))

    if "toughness" in card:
        filename_builder.append(card["toughness"].replace("*", "x"))

    if "Artifact" in card["type_line"]:
        filename_builder.append("a")

    if "Enchantment" in card["type_line"]:
        filename_builder.append("e")

    filename_builder.append(card["name"].replace(" ", "_").lower())

    return ("_".join(filename_builder), card["name"].replace(" ", "_").lower(), "@" + card["artist"])

# Main workflow
def main(directory):
    process_files(directory)

# Run the script
directory_path = "../res/editions/"  # Replace with your actual directory
main(directory_path)
