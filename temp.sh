#!/usr/bin/env bash

red=$(tput setaf 1)
green=$(tput setaf 2)
yellow=$(tput setaf 3)
reset=$(tput sgr0)

if [ $# -lt 2 ]; then
    echo "Sets the addon-hifi flag to true/false on the given account username(s)"
    echo "This script will set addon-hifi to 'true' ONLY if the account is an employee account. Use the -f flag to force it for non-employee accounts."
    echo 'Usage: ./set_hifi_on_account.sh [true|false] (-f) [username] (username2) (username3) (...)'
    exit 1
elif [ "$1" != "true" ] && [ "$1" != "false" ]; then
    echo "First arugment MUST be either true or false!"
    exit 1
fi

numErrors=0
numWarnings=0

setOnEmployeeOnly=true

if [ "$1" == "false" ]; then 
    setOnEmployeeOnly=false
else
    for arg in "$@"; do
        if [ "$arg" == "-f" ]; then
            setOnEmployeeOnly=false
            break
        fi
    done
fi

firstArg=true
for username in "$@"; do
    if [ $firstArg != "true" ] && [ "$username" != "-f" ]; then
        echo ""
        echo "${green}Processing $username${reset}"
        if [[ "$username" == *"@"* ]]; then
            echo "> Email detected, fetching username"
            accountResponse=$(jhurl -s services.gew1 -X "GET" "hm://userdata/account?email=$username" --service test 2>&1)
            
            # Handle possile errors a bit more gracefully
            if [[ "$accountResponse" == "No hosts found"* ]]; then
                echo "> ${red}ERROR${reset}: The userdata service appears unreachable! Are you connected to the Spotify VPN?"
                echo "${red}Halting script!${reset}"
                exit 1
            elif [[ "$accountResponse" != *"\"username\":"* ]]; then
                echo "> ${red}ERROR${reset}: Account not found email: $username!"
                numErrors=$((numErrors+1))
                continue
            fi 
            
            usernames=$(echo "$accountResponse" | grep -o -E '"username":"[^"]*"' | awk -F\: '{print $2}' | sed 's/"//g' 2>&1)
            usernameList=()
            while read -r line; do
                usernameList+=("$line")
            done <<< "$usernames"
            if [ "${#usernameList[@]}" -gt 1 ]; then
                echo "> ${yellow}WARNING${reset}: Found ${#usernameList[@]} username(s), but will process ONLY 1st username"
                numWarnings=$((numWarnings+1))
            fi
            
        else
            usernameList=()
            usernameList+=("$username")
            if [ "$setOnEmployeeOnly" = true ]; then 
                accountResponse=$(jhurl -s services.gew1 -X "GET" "hm://userdata/account?username=$username" --service test 2>&1)
            fi
        fi
        
        #for user in "${usernameList[@]}"; do
        # temporarily: Only use 1st username
        user=$(echo "${usernameList[0]}" | awk '{print tolower($0)}')
        echo "> Processing username: \"$user\" "

        if [ "$setOnEmployeeOnly" = true ]; then 
            isEmployee=$(echo "$accountResponse" | grep -o -E '"employee":[^,]*' | awk -F\: '{print $2}' | sed 's/"//g' 2>&1)
            if [ "$isEmployee" != "true" ]; then 
                echo "> ${red}ERROR${reset}: Account with username $username is NOT an Employee Account! Skipping setting HiFi on it!"
                numErrors=$((numErrors+1))
                continue 
            fi
        fi

        setFlagResponse=$(echo "{\"username\":\"$user\", \"attributes\" : {\"addon-hifi\": \"$1\"}}" | jhurl -s services.gew1 -X "PUT" "hm://userdata/attributes" --service test 2>&1)

        # Handle possible errors
        if [[ "$setFlagResponse" == "No hosts found"* ]]; then
            echo "${red}ERROR${reset}: The service appears unreachable - are you connected to the Spotify VPN?"
            echo "${red}Halting script.${reset}"
            exit 1
        fi

        # Check if response is OK
        if [[ "$setFlagResponse" == *"200 OK"* ]]; then
            echo "> 200 OK"
        elif [[ "$setFlagResponse" == *"Account not found username"* ]]; then
            numErrors=$((numErrors+1))
            echo "> ${red}ERROR${reset}: Account not found username \"$user\"!"
        else
            numErrors=$((numErrors+1))

            echo "> ${red}ERROR${reset}: Generic error for username $user!! Full reponse:"
            echo "$setFlagResponse"
        fi
        #done
    fi
    firstArg=false
done
echo ""
echo "Done"
echo "${red}Errors${reset}  : $numErrors"
echo "${yellow}Warnings${reset}: $numWarnings"