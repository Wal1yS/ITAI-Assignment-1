# SNA Lab Report Template

**Filename:** SNA-Lab_2_<Surname>_<Name>.md

---

## Header

**Course:** System and Network Administration  
**Lab:** Lab 2 — The filesystem, command line, and file manipulation  
**Student:** <Full Name>  
**Group:** <Group>

**Environment:**  
*Brief explanation of the lab environment:*  
<e.g., Ubuntu 22.04 LTS running in VirtualBox. Allocated 4GB RAM.>

---

## 1) Grading requirements

*   Each lab has 10 points as maximum. To pass the lab, it should have minimum 6 points.
*   By the end of the course every lab should be passed.
*   If lab was not done/passed in time (before an official deadline), student needs to do a retake where score can be 6 as the maximum.

---

## 2) Tasks and Implementation

**Rule:** For each task, keep a simple flow: Task → Commands → Proof of work → Short explanation.

### Part 1: Filesystem

#### 1. View Filesystem Info
```bash
lsblk -f
```
**What it does:** Lists all storage devices (blocks) attached to the system and shows information about their file systems (like names, types, and labels).  
**Flags:**
*   `-f`: Output info about filesystems (UUIDs, labels, types).

**Proof:** see Figure 1.

![Figure 1: Block devices info](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 1: Block devices info*

#### 2. View Device Files
```bash
ls -lah /dev
```
**What it does:** Lists all the device files located in the `/dev` directory. In Linux, everything is a file, including hardware devices.  
**Flags:**
*   `-l`: Long format (permissions, dates).
*   `-a`: Show all files (including hidden ones).
*   `-h`: Human-readable sizes.

**Proof:** see Figure 2.

#### 3. View Memory Info
```bash
cat /proc/meminfo
```
**What it does:** Reads the file that contains real-time statistics about system memory usage directly from the kernel.  
**Proof:** see Figure 3.

![Figure 3: Memory info](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 3: Memory info*

---

### Part 2: echo, pipes, redirects and quotes

#### 1. Check Location
```bash
pwd
```
**What it does:** Shows the current directory path.  
**Proof:** see Figure 4.

#### 2. Create Directory
```bash
mkdir mydocs
```
**What it does:** Creates a new folder named `mydocs`.  
**Proof:** see Figure 4.

#### 3. Change Directory
```bash
cd mydocs
```
**What it does:** Moves inside the `mydocs` folder.  
**Proof:** see Figure 4.

#### 4. Create Empty File
```bash
touch textproc
```
**What it does:** Creates an empty file named `textproc` (or updates its timestamp if it already exists).  
**Proof:** see Figure 4.

#### 5. Verify Creation
```bash
ls -la
```
**What it does:** Lists files to confirm `textproc` was created.  
**Proof:** see Figure 4.

![Figure 4: Creating directory and file](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 4: Creating directory and file*

#### 6. Recursive Copy
```bash
cd ..
cp -R mydocs/textproc ~
```
**What it does:** Goes back one folder level, then copies the `textproc` file from `mydocs` into the user's home directory (`~`).  
**Flags:**
*   `-R`: Recursive (usually for directories, but works here to denote deep copying).

**Proof:** see Figure 5.

#### 7. Echo to File (Overwrite)
```bash
echo "My name is $USER and my home directory is $HOME" > simple_echo
cat simple_echo
```
**What it does:** Puts a text string containing your username and home path into a file named `simple_echo`. The `>` symbol creates or **overwrites** the file.  
**Proof:** see Figure 6.

#### 8. Echo to File (Append)
```bash
echo "My Salary is 100" >> simple_echo
cat simple_echo
```
**What it does:** Adds new text to the end of the existing file `simple_echo` without deleting previous content. The `>>` symbol means **append**.  
**Proof:** see Figure 6.

#### 9. Copy via Redirection
```bash
cat simple_echo > new_echo
```
**What it does:** Reads content from `simple_echo` and redirects it into a new file `new_echo`. It works exactly like a copy command.  
**Proof:** see Figure 6.

![Figure 6: Redirection examples](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 6: Redirection examples*

#### 10. Generate Error
```bash
cat nofile
```
**What it does:** Tries to read a file that doesn't exist to generate a "No such file or directory" error on the screen.  
**Proof:** see Figure 7.

#### 11. Redirect Error (stderr)
```bash
cat nofile 2> error_out
```
**What it does:** Tries to read a non-existent file, but instead of showing the error on screen, it sends the error message (Stream #2) into a file named `error_out`.  
**Proof:** see Figure 7.

#### 12. Redirect Everything
```bash
cat nofile > allout 2>&1
```
**What it does:** Redirects standard output (`>`) to `allout` and tells standard error (`2`) to go to the same place as standard output (`&1`). Both success and error messages go to the same file.  
**Proof:** see Figure 7.

![Figure 7: Error handling](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 7: Error handling*

#### 13. Heredoc (Input block)
```bash
cat << foobar
Hello foobar
foobar
```
**What it does:** Allows you to type multiple lines of text in the terminal. It keeps reading until you type the specific "stop word" (here it is `foobar`).  
**Proof:** see Figure 8.

#### 14. Numbering Lines (from file)
```bash
nl < fragmented.txt
```
**What it does:** Reads text from `fragmented.txt` (using `<` input redirection) and displays it with line numbers added.  
**Proof:** see Figure 9.

#### 15. Sort Keyboard Input
```bash
sort << end
```
**What it does:** Lets you type a list of words. When you type `end`, it stops reading and immediately sorts everything you typed alphabetically.  
**Proof:** see Figure 10.

#### 16. Sort Keyboard Input to File
```bash
sort << end > sorted
```
**What it does:** Same as above, but instead of showing the result on screen, it saves the sorted list into a file named `sorted`.  
**Proof:** see Figure 10.

#### 17. Complex Pipeline
```bash
sort << end | nl -ba > sorted_numbered
```
**What it does:** 
1. Reads input until "end".
2. Sorts it.
3. Sends result to `nl` to add numbers.
4. Saves final result to `sorted_numbered`.

**Flags:**
*   `-ba`: Number **all** lines, including empty ones.

**Proof:** see Figure 11.

![Figure 11: Complex pipeline](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 11: Complex pipeline*

#### 18. Escape Characters
```bash
echo -e "Next is the \nNew line"
```
**What it does:** Prints text where `\n` is interpreted as a real "New Line" (enter key).  
**Flags:**
*   `-e`: Enable interpretation of backslash escapes.

**Proof:** see Figure 12.

#### 19. Pipe and Grep
```bash
cat /etc/passwd | grep $USER
```
**What it does:** Reads the user database file and filters the output to show only the line containing your username.  
**Proof:** see Figure 12.

#### 20. Compound Command
```bash
echo -e "hello\na new day\nsee the world\ncall sign" > newfile.txt && sort newfile.txt
```
**What it does:** Creates a file with 4 lines of text. If that succeeds (`&&`), it immediately runs the sort command on that file.  
**Proof:** see Figure 13.

---

### Part 3: Heads and tails, cat and tac

#### 1. Prepare Data
```bash
echo -e "for1\nfor2\nfor3\nfor4\nfor5\nfor6\nfor7\nfor8\nfor9\nfor10" > numbers
```
**What it does:** Creates a file named `numbers` containing 10 lines (for1 to for10).  
**Proof:** see Figure 14.

#### 2. Tail (Bottom)
```bash
tail -n5 numbers
```
**What it does:** Shows only the **last** 5 lines of the file.  
**Flags:**
*   `-n5`: Number of lines to show.

**Proof:** see Figure 14.

#### 3. Head (Top)
```bash
head -n3 numbers
```
**What it does:** Shows only the **first** 3 lines of the file.  
**Flags:**
*   `-n3`: Number of lines to show.

**Proof:** see Figure 14.

#### 4. Tac (Reverse Cat)
```bash
tac numbers
```
**What it does:** Displays the file content, but reverses the order of lines (last line becomes first). It is "cat" spelled backwards.  
**Proof:** see Figure 14.

![Figure 14: Head, Tail and Tac](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 14: Head, Tail and Tac*

---

### Part 4: Cutting comments

#### 1. Cut Columns (Delimited)
```bash
cut -d: -f1,4,5 /etc/passwd
```
**What it does:** Extracts specific columns from the password file.  
**Flags:**
*   `-d:`: Use colon (`:`) as the separator between columns.
*   `-f1,4,5`: Show only fields #1 (user), #4 (GID), and #5 (Description).

**Proof:** see Figure 15.

#### 2. Cut with Space
```bash
cut -d" " -f1,2 /etc/mtab
```
**What it does:** Shows mounted filesystems types and paths.  
**Flags:**
*   `-d" "`: Use space as the separator.
*   `-f1,2`: Show first two columns.

**Proof:** see Figure 15.

#### 3. Cut from Command Output
```bash
uname -a | cut -d" " -f1,3,11,12
```
**What it does:** Takes system info and cuts out just the Kernel name, Version, and Time.  
**Proof:** see Figure 16.

![Figure 16: Cutting output](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 16: Cutting output*

---

### Part 5: Unique lines and sorting

#### 1. Create Data File
```bash
vi unique.txt
# (Pasted the text from the task)
```
**What it does:** Opens a text editor to create a file with duplicate lines for testing.  
**Proof:** see Figure 17.

#### 2. Count Occurrences
```bash
uniq -c unique.txt
```
**What it does:** Collapses adjacent duplicate lines and shows a number indicating how many times each line appeared.  
**Flags:**
*   `-c`: Count prefixes.

**Proof:** see Figure 18.

#### 3. Show Only Unique
```bash
uniq -u unique.txt
```
**What it does:** Shows ONLY the lines that appear exactly once. Lines that have duplicates are hidden completely.  
**Flags:**
*   `-u`: Unique only.

**Proof:** see Figure 18.

#### 4. Sort File
```bash
sort /etc/passwd
```
**What it does:** Sorts the content of the password file alphabetically (A-Z) based on the first character of the line.  
**Proof:** see Figure 19.

#### 5. Sort by Key
```bash
sort -t: -k1 /etc/passwd
```
**What it does:** Sorts the file specifically using the first column (username).  
**Flags:**
*   `-t:`: Use colon as separator.
*   `-k1`: Sort using key #1 (the first column).

**Proof:** see Figure 19.

![Figure 19: Sorting](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 19: Sorting*

---

### Part 6: Bash helpers

#### 1. History
```bash
history
```
**What it does:** Prints a numbered list of commands you have typed in this terminal session.  
**Proof:** see Figure 20.

#### 2. Execute by Number
```bash
!2
```
**What it does:** Automatically re-runs the command that is at line #2 in your history list.  
**Proof:** see Figure 20.

#### 3. Ping
```bash
ping 127.0.0.1
```
**What it does:** Checks network connectivity to your own computer (localhost). You stop it with `Ctrl+C`.  
**Proof:** see Figure 21.

#### 4. Repeat Last Command
```bash
!!
```
**What it does:** Re-runs the very last command you typed (in this case, it runs ping again).  
**Proof:** see Figure 21.

![Figure 21: Ping and history repeat](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 21: Ping and history repeat*

---

## 3) Answers to Questions

### 1. Filesystem

**Q1: How many inodes are in use on your system?**  
**Answer:** I checked this using the command `df -i`. The column `IUsed` shows the number of used inodes for each partition. For the root partition `/`, the number is visible there.

**Q2: What is the filesystem type of the EFI partition?**  
**Answer:** The filesystem type is **vfat** (FAT32). I found this by running `lsblk -f`, which lists the FSTYPE for the boot partition.

**Q3: What device is mounted at your root `/` directory? Show proof.**  
**Answer:** The device is `<your-device>` (e.g., `/dev/nvme0n1p2` or `/dev/sda2`). I verified this by looking at the output of `lsblk` or `df -h /`, where the Mountpoint `/` corresponds to the device name.

**Q4: What is your partition UUID?**  
**Answer:** I found the UUID by running `lsblk -f`. It is a long string of unique characters assigned to the partition (e.g., `a1b2-c3d4...`).

**Q5: What is the function of `/dev/zero`?**  
**Answer:** It is a special file that provides an infinite stream of "null" characters (zeros). It is useful for overwriting data to clean it or creating dummy files of a specific size (like we did with the swap file in Lab 1).

### 2. Command line and file manipulation

**Q1: Explain the role of the Pipe `|` in this command `cat /etc/apt/sources.list | less`.**  
**Answer:** The pipe takes the output (stdout) of the command on the left (`cat`) and feeds it as input (stdin) to the command on the right (`less`). This allows us to scroll through a long file instead of dumping it all on the screen at once.

**Q2: What does section 5 in man mean? And how can you find it?**  
**Answer:** Section 5 of the manual is for **File Formats**. It explains the syntax of configuration files (like `/etc/passwd`). I can access it by typing `man 5 passwd`. I found this info by typing `man man`.

**Q3: What is the full file path of `ls` on your machine? How did you find it?**  
**Answer:** The path is `/usr/bin/ls` (or `/bin/ls`). I found it by running the command `which ls` or `type -a ls`.

**Q4: Show two ways of renaming a file `test_file.tot` to `test_file.txt`.**  
**Answer:**
1.  Using move: `mv test_file.tot test_file.txt`
2.  Using copy and remove: `cp test_file.tot test_file.txt && rm test_file.tot`

**Q5: Create a compound command for the string task...**  
**Answer:**
```bash
echo -e "The location...\n..." | sort | uniq > result.txt && echo $USER >> result.txt
```
*(Note: Replace the echo string with the full text provided in the prompt).*

**Q6: What can you do to discard the output from the command `ping`? Discard stderr too.**  
**Answer:** I redirect everything to the "black hole" device `/dev/null`.
Command: `ping 127.0.0.1 > /dev/null 2>&1`.

**Q7: Show how you can sort input, append line numbers, and save the sorted result to a file. Add line numbers to empty lines too.**  
**Answer:**
```bash
sort | nl -ba > sorted_result.txt
```
(I type input, press Ctrl+D to finish, and it saves).

**Q8: Create directory and list ways to go from `/usr/share` to `/home/$USER/testdir`.**  
**Answer:**
1.  Absolute path: `cd /home/username/testdir`
2.  Relative path: `cd ../../home/username/testdir`
3.  Home shortcut: `cd ~/testdir`
4.  Variables: `cd $HOME/testdir`

**Q9: Write a pipe that will result with a unique list of commands/shell from `/etc/passwd`.**  
**Answer:**
```bash
cut -d: -f7 /etc/passwd | sort | uniq
```
(This cuts the 7th column, sorts them, and removes duplicates).

#### Bonus Questions

**Q10: Find all man pages that contain word `malloc`. Just a list of files.**  
**Answer:**
```bash
man -k -w malloc
```
(This searches keywords and `-w` shows the path to the files).

**Q11: Write a one-liner `grep`...**  
**Answer:**
```bash
pattern="mypattern"; grep -q "$pattern" filename && echo "Was found!" || echo "Wasn't found..."
```
This uses logical operators: `&&` runs if grep succeeds (found), `||` runs if grep fails (not found).

---

## 4) References

1.  Ubuntu Manpages. [http://manpages.ubuntu.com/](http://manpages.ubuntu.com/)
2.  GNU Coreutils manual. [https://www.gnu.org/software/coreutils/manual/](https://www.gnu.org/software/coreutils/manual/)
3.  Linux Command Line (TLCL). [https://linuxcommand.org/tlcl.php](https://linuxcommand.org/tlcl.php)
