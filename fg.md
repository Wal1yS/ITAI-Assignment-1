SNA Lab 3 Report

Course: System and Network Administration Lab: Lab 3: File permissions and text filtering editors Student: Valerii Tiniakov Group: B24-SD-03 Environment: Ubuntu 24.04 LTS running on my laptop.
Part 1: Exercise 1 - File Permissions
1. Preparation and Listing
Bash

sudo su
cd /home
mkdir /home/shared
ls -l

What it does: Switches to the root user, navigates to the /home directory, creates a new directory named shared, and lists the contents in long format to verify permissions. Flags:

    -l: Long format (shows permissions, ownership, size, and modification date). Proof: see Figure 1.

2. Creating Groups and Users
Bash

groupadd users
less /etc/group
useradd -m -G users david
useradd -m paul
passwd david
passwd paul

What it does: Creates a group named users (if it doesn't exist), views the group file, creates two users (david and paul), and sets their passwords. David is added to the users group; Paul is not. Flags:

    -m: Creates the user's home directory.

    -G: Specifies a list of supplementary groups which the user is also a member of. Proof: see Figure 2.

3. Changing Group Ownership
Bash

chgrp users /home/shared

What it does: Changes the group ownership of the directory /home/shared to the group users. Proof: see Figure 3.
4. Setting Directory Permissions
Bash

chmod 750 /home/shared

What it does: Sets permissions for /home/shared. Permissions breakdown:

    7 (User/Owner): Full access.

    5 (Group): Read and Execute access.

    0 (Others): No access. Proof: see Figure 4.

5. Testing Permissions (As David)
Bash

sudo -u david -s
id
cd /home/shared
touch davids_file
exit

What it does: Switches to user david, checks identity, enters the shared directory, and attempts to create a file. The touch command fails because the group permissions are only Read/Execute, meaning David can enter the directory but cannot write (create files) in it. Flags:

    -u: Specifies the target user to switch to.

    -s: Runs the shell specified for the target user. Proof: see Figure 5.

6. Testing Permissions (As Paul)
Bash

sudo -u paul -s
id
cd /home/shared
exit

What it does: Switches to user paul. Paul tries to enter /home/shared. This fails because Paul is not the owner and not in the users group; he falls under "Others", which has No access. Flags:

    -u: Specifies the target user to switch to.

    -s: Runs the shell specified for the target user. Proof: see Figure 6.

7. Setting SGID and Write Permissions
Bash

chmod 770 /home/shared
chmod 2770 /home/shared
ls -l /home

What it does:

    Gives the Group (users) Write access.

    Sets the SGID (Set Group ID) bit. Any new file created inside this directory will inherit the group users instead of the creator's primary group. Permissions breakdown (for 770):

    7 (User): Full access.

    7 (Group): Full access.

    0 (Others): No access. Permissions breakdown (for 2770):

    2 (Special bit): SGID (Set Group ID).

    7 (User): Full access.

    7 (Group): Full access.

    0 (Others): No access. Flags:

    -l: Long format (shows permissions to verify the s bit). Proof: see Figure 7.

8. Verifying SGID (As David)
Bash

sudo -u david -s
cd /home/shared
touch newfile
ls -l

What it does: David creates a file. Because SGID is set on the parent directory, newfile is automatically owned by the group users, not David's primary group. Flags:

    -u: Specifies the target user.

    -s: Runs the shell.

    -l: Long format. Proof: see Figure 8.

9. Changing Ownership (chown/chgrp)
Bash

chown david.david newfile
chown .users newfile

What it does:

    Changes owner to david and group to david.

    Changes the group back to users. Proof: see Figure 9.

10. Understanding Umask
Bash

cd
umask
mkdir umask_test
ls -ld umask_test
touch umask_file
ls -l umask_file

What it does: Checks default permissions. With default umask 0002: Permissions breakdown:

    Directory (775):

        7 (User): Full access.

        7 (Group): Full access.

        5 (Others): Read and Execute access.

    File (664):

        6 (User): Read and Write access.

        6 (Group): Read and Write access.

        4 (Others): Read only access. Flags:

    -l: Long format.

    -d: List directory entries instead of contents. Proof: see Figure 10.

11. Changing Umask
Bash

umask 0022
touch newfile1
mkdir newdir1
ls -l

What it does: Sets umask to 0022. Permissions breakdown:

    Directory (755):

        7 (User): Full access.

        5 (Group): Read and Execute access.

        5 (Others): Read and Execute access.

    File (644):

        6 (User): Read and Write access.

        4 (Group): Read only access.

        4 (Others): Read only access. Flags:

    -l: Long format. Proof: see Figure 11.

Part 2: Exercise 2 - Text Filtering Editors
1. Preparation
Bash

cp /etc/fstab ~
cp /etc/passwd ~

What it does: Copies the filesystem table and password file to the home directory for safe manipulation. Proof: see Figure 12.
2. Grep: Searching
Bash

grep "systemd" passwd
grep -n "systemd" passwd
grep -v "systemd" passwd

What it does:

    Finds lines containing "systemd".

    Finds lines and prints line numbers.

    Inverts match: prints lines not containing "systemd". Flags:

    -n: Prefixes each line of output with the 1-based line number.

    -v: Inverts the sense of matching, to select non-matching lines. Proof: see Figure 13.

3. Grep: Context
Bash

grep -A 5 "systemd" passwd
grep -B 3 "systemd" passwd
grep -C 5 "systemd" passwd

What it does: Prints the matching line plus context lines around it. Flags:

    -A: Prints NUM lines of trailing context After matching lines.

    -B: Prints NUM lines of leading context Before matching lines.

    -C: Prints NUM lines of output Context (both before and after). Proof: see Figure 14.

4. Grep: Regex
Bash

grep -P "(systemd|root)" passwd

What it does: Uses Perl-compatible regular expressions to find lines containing either "systemd" OR "root". Flags:

    -P: Interprets the pattern as a Perl-Compatible Regular Expression (PCRE). Proof: see Figure 15.

5. AWK: Basic Filtering and Substitution
Bash

awk '/systemd/{print $0}' passwd
awk '{gsub(/systemd/, "NEWSYSTEMD")}{print}' passwd

What it does:

    Works like grep: finds "systemd" and prints the whole line.

    Substitutes "systemd" with "NEWSYSTEMD" globally in the text and prints the result. Proof: see Figure 16.

6. AWK: Formatting and Fields
Bash

awk 'BEGIN {print "PASSWD FILE\n--------------"} {print} END {print "--------------\nEND OF PASSWD FILE"}' passwd
awk -F ":" '{print $1, $6, $7}' passwd

What it does:

    Adds a custom header before processing and a footer after processing.

    Splits lines by colon (:) and prints the Username ($1), Home Dir ($6), and Shell ($7). Flags:

    -F: Sets the field separator (delimiter). Proof: see Figure 17.

7. AWK: Logic
Bash

awk -F ":" '{ if ($3 > 100) {print $0} }' passwd

What it does: Checks if the UID (3rd field) is numerically greater than 100. If true, prints the line. Flags:

    -F: Sets the field separator (delimiter). Proof: see Figure 18.

8. SED: Printing and Substitution
Bash

sed -n '/systemd/p' passwd
sed -n 's/systemd/NEWSYSTEMD/p' passwd

What it does:

    Finds lines with "systemd" and prints them.

    Finds "systemd", substitutes it with "NEWSYSTEMD", and prints the result. Flags:

    -n: Suppresses automatic printing of pattern space (quiet mode). Proof: see Figure 19.

9. SED: Ranges and Global Editing
Bash

sed '1 s/root/NOTROOT/' passwd
sed '2,4 s/bin/NOBIN/g' passwd
sed -n '5,/systemd/p' passwd

What it does:

    On line 1 only, replace "root" with "NOTROOT".

    From lines 2 to 4, replace "bin" with "NOBIN" globally.

    Print lines starting from line 5 until the first line containing "systemd" is found. Flags:

    -n: Suppresses automatic printing of pattern space. Proof: see Figure 20.

10. SED: Contextual Replacement
Bash

sed '/efi/ s/sda/hda/g' fstab
sed '/root/ s/:/;/g' passwd

What it does:

    Finds lines containing "efi", then replaces "sda" with "hda" on those lines.

    Finds lines containing "root", then replaces colons with semicolons. Proof: see Figure 21.

11. SED: Deletion
Bash

vi unique.txt # (Created file with sample text)
sed '2,3 d' unique.txt
sed '/^This/ d' unique.txt

What it does:

    Deletes lines 2 through 3.

    Deletes any line starting with the word "This". Proof: see Figure 22.

Part 3: Questions to answer
1. File permissions

1.1 Create a file called group_test in /home/shared as a user david. Answer:
Bash

sudo -u david touch /home/shared/group_test

Flags:

    -u: Specifies the target user. Proof: see Figure 23.

1.2 Change the owner of group_test to paul while keeping the group ownership as users. Answer:
Bash

sudo chown paul:users /home/shared/group_test

Proof: see Figure 24.

1.3 Log in as paul and verify if he can modify group_test. Explain why. Answer:
Bash

sudo -u paul -s
echo "modification" >> /home/shared/group_test

Yes, Paul can modify the file. Reason: Paul is now the Owner of the file. Even though he is not in the users group, the Owner permissions take precedence. If file permissions are 644: Permissions breakdown:

    6 (Owner - Paul): Read and Write access.

    4 (Group): Read only access.

    4 (Others): Read only access. Flags:

    -u: Specifies the target user.

    -s: Runs the shell. Proof: see Figure 25.

1.4 Now change the group ownership to a new group called shared_group (which you need to create) and verify access control. Answer:
Bash

sudo groupadd shared_group
sudo chown :shared_group /home/shared/group_test
# Paul (Owner) can still access.
# David (previously accessed via 'users' group) can NO LONGER write/modify if he is not in shared_group.

Proof: see Figure 26.
2. Text filtering editors

Data Preparation: Created server-data.log with the provided log content.

2.1. View only error and warning messages in server-data.log. Show how you can do this with grep and awk. Answer: Grep: grep -E "ERROR|WARNING" server-data.log AWK: awk '/ERROR|WARNING/ {print}' server-data.log Flags:

    -E (grep): Interpret pattern as extended regular expressions (allows | for OR). Proof: see Figure 27.

2.2. View every line except lines with informational messages. Answer:
Bash

grep -v "INFO" server-data.log

Flags:

    -v: Invert match (select non-matching lines). Proof: see Figure 28.

2.3. Count how many error messages are in the log. Answer:
Bash

grep -c "ERROR" server-data.log

Flags:

    -c: Suppress normal output; instead print a count of matching lines. Proof: see Figure 29.

2.4. Hide the IP addresses. Replace all IP addresses with xxx.xxx.xxx.xxx/xx and save the output to a file newlog.log. Show the output. Answer:
Bash

sed -E 's/[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+\/[0-9]+/xxx.xxx.xxx.xxx\/xx/g' server-data.log > newlog.log
cat newlog.log

Flags:

    -E: Use extended regular expressions (allows using + for "one or more"). Proof: see Figure 30.

2.5. Write a single regular expression to match the following lines in server-data.log. Show the full command and regex used. Answer: The regex matches the timestamp, the specific daemon, the log level (INFO/ERROR/WARNING), and the message structure ending with an IP.
Bash

grep -E "^[0-9]{4}/[0-9]{2}/[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2} wazuh-remoted: (INFO|ERROR|WARNING): .* from: '[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+/[0-9]+'$" server-data.log

Flags:

    -E: Use extended regular expressions (needed for {} quantifiers and () grouping). Proof: see Figure 31.

2.6. Bonus: Write a sed one-liner that will show stack traces lines in the following fashion. Answer: This command uses capture groups (.*) to isolate the method path, file name, and line number, then rearranges them in the replacement string using \1, \2, etc.
Bash

sed -E 's/at (.*)\.(.*)\((.*):([0-9]+)\)/Exception occured inside method `\1.\2` from file `\3` on line `\4`. The file was written in `java`./' stacktrace.log

(Assuming the input lines are saved in stacktrace.log) Flags:

    -E: Use extended regular expressions (allows using capture groups (...) without escaping parentheses). Proof: see Figure 32.
