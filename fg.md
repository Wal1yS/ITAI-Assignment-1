# LAB TEMPLATE - SYSTEM AND NETWORK ADMINISTRATION

# SNA Lab Report Template

**Filename:** SNA-Lab_1_<Surname>_<Name>.md

---

## Header

**Course:** System and Network Administration  
**Lab:** Lab 1 — Introduction to Linux and OS main components  
**Student:** <Full Name>  
**Group:** <Group>

**Environment:**  
*Brief explanation of the lab environment:*  
<e.g., Ubuntu 22.04 LTS running in VirtualBox on Windows 11 host. Allocated 4GB RAM, 20GB Storage.>

---

## 1) Grading requirements

*   Each lab has 10 points as maximum. To pass the lab, it should have minimum 6 points.
*   By the end of the course every lab should be passed.
*   If lab was not done/passed in time (before an official deadline), student needs to do a retake where score can be 6 as the maximum.

---

## 2) Tasks and Implementation

**Rule:** For each task, keep a simple flow: Task → Commands → Proof of work (includes but not limited to screenshots of terminal, networks schemes, log and configuration files) → Short explanation (in your own words).

### Exercise 1 — Finding your way around Linux

#### 1. Check Distribution
```bash
lsb_release -a
```
**What it does:** Prints distribution-specific information such as the description, release number, and codename.  
**Flags:** `-a` prints all available information.  
**Proof:** see Figure 1.

![Figure 1: Distribution Information](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 1: Distribution Information*

#### 2. Check Current User
```bash
whoami
```
**What it does:** Prints the username associated with the current effective user ID.  
**Proof:** see Figure 2.

#### 3. View Logged In Users
```bash
users
```
**What it does:** Prints the usernames of users currently logged in to the current host.  
**Proof:** see Figure 2.

#### 4. Current Working Directory
```bash
pwd
```
**What it does:** Prints the full filename of the current working directory.  
**Proof:** see Figure 2.

![Figure 2: User and Directory checks](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 2: User and Directory checks*

#### 5. List Content
```bash
ls -la
```
**What it does:** Lists information about the files in the current directory, including hidden files and permissions.  
**Flags:** 
*   `-l`: use a long listing format (shows permissions, owner, size).
*   `-a`: do not ignore entries starting with `.` (hidden files).

**Proof:** see Figure 3.

![Figure 3: Directory listing](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 3: Directory listing*

#### 6. Navigation
```bash
cd Downloads/
pwd
```
**What it does:** Changes the current directory to `Downloads` and confirms the new location.  
**Proof:** see Figure 4.

#### 7. Shell Information
```bash
cat /etc/shells
echo "$SHELL"
```
**What it does:** The first command reads the file containing valid login shells. The second command prints the path to the currently active shell interpreter.  
**Proof:** see Figure 4.

![Figure 4: Navigation and Shell info](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 4: Navigation and Shell info*

---

### Exercise 2 — SWAP Management

#### 1. Check Swap Status
```bash
swapon --show
```
**What it does:** Displays the current swap usage and active swap devices.  
**Proof:** see Figure 5.

#### 2. Create Swap File
```bash
dd if=/dev/zero of=/swapfile bs=1024 count=2097152
```
**What it does:** Creates a file named `/swapfile` filled with zeros with a size of 2GB.  
**Flags:**
*   `if`: Input file (source of data, `/dev/zero`).
*   `of`: Output file (destination).
*   `bs`: Block size (bytes).
*   `count`: Number of blocks to copy.

**Proof:** see Figure 5.

#### 3. Configure and Enable Swap
```bash
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
```
**What it does:** Sets secure permissions (only root can read/write), sets up a Linux swap area on the file, and enables it.  
**Proof:** see Figure 5.

![Figure 5: Swap creation and activation](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 5: Swap creation and activation*

#### 4. Verify Swap and Persistence
```bash
free -h
cat /etc/fstab
```
**What it does:** `free -h` shows memory usage in human-readable format. Checking `/etc/fstab` confirms that the configuration for the swap file was added for persistence after reboot.  
**Proof:** see Figure 6.

![Figure 6: Swap verification](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 6: Swap verification*

---

### Exercise 3 — GPT Partition

#### 1. List Storage Devices
```bash
fdisk -l
```
**What it does:** Lists the partition tables for the specified devices.  
**Proof:** see Figure 7.

![Figure 7: Disk listing](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 7: Disk listing*

#### 2. MBR Dump
```bash
dd if=/dev/sda bs=512 count=1 skip=0 > lba.0
hexedit lba.0
```
**What it does:** Dumps the Protective MBR (LBA 0) and displays it in hexadecimal format.  
**Flags:**
*   `skip=0`: Starts reading from the beginning of the disk.

**Proof:** see Figure 8.

![Figure 8: MBR Hex Dump](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 8: MBR Hex Dump*

#### 3. GPT Header Dump
```bash
dd if=/dev/sda bs=512 count=1 skip=1 > lba.1
hexedit lba.1
```
**What it does:** Dumps the GPT Header (LBA 1) and displays it.  
**Flags:**
*   `skip=1`: Skips the first block (MBR) to reach the GPT header.

**Proof:** see Figure 9. Note the "EFI PART" signature.

![Figure 9: GPT Header Hex Dump](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 9: GPT Header Hex Dump*

---

### Exercise 4 — UEFI Booting

#### 1. Boot Order
```bash
efibootmgr -v
```
**What it does:** Displays the EFI Boot Manager configuration, including the boot order and active boot entries.  
**Proof:** see Figure 10.

![Figure 10: EFI Boot Manager](INSERT_PATH_TO_SCREENSHOT_HERE)
*Figure 10: EFI Boot Manager*

---

## 3) Answers to Questions (if any)

### 1. Introduction to Linux

**Q1: What is your machine hostname? How did you check it?**  
**Answer:** The hostname is `<your-hostname>` (likely `ubuntu` or `ubuntu-vm`). I checked it using the command `hostname` (or by viewing `/etc/hostname`).

**Q2: What is the difference between `/bin/bash` and `/bin/sh`?**  
**Answer:** `/bin/sh` is typically a symlink to a POSIX-compliant shell (like `dash` on Ubuntu) designed for speed and standard script execution. `/bin/bash` is the Bourne Again SHell, which is more feature-rich (supports command history, advanced auto-completion, arrays) and is generally used for interactive sessions.

**Q3: Explain all the details of the output from the command `uname -a`.**  
**Answer:** The output includes: the Kernel name (Linux), the Network node hostname, the Kernel release (e.g., 5.15.0-generic), the Kernel version (build date/time), the Machine hardware architecture (x86_64), and the Operating System name (GNU/Linux).

**Q4: What command typically shows you the manual for POSIX compliant tools on the Linux operating system?**  
**Answer:** The command is `man` (e.g., `man ls`).

### 2. GPT

**Q1: What is fdisk utility used for?**  
**Answer:** `fdisk` is a command-line utility used for disk partitioning functions, such as viewing, creating, resizing, deleting, and changing partitions on a hard drive.

**Q2: Show the bootable device(s) on your machine, and identify which partition(s) are bootable.**  
**Answer:** The bootable device is typically `/dev/sda`. The bootable partition is the EFI System Partition (ESP), often listed as `/dev/sda1` (or similar) with the type "EFI System". (See Figure 7).

**Q3: What is logical block address?**  
**Answer:** Logical Block Addressing (LBA) is a common scheme used for specifying the location of blocks of data stored on computer storage devices, replacing the older cylinder-head-sector (CHS) scheme.

**Q4: Why did we specify the `count`, the `bs`, and the `skip` options when using dd?**  
**Answer:**
*   `bs=512`: Defines the block size as 512 bytes (standard sector size).
*   `count=1`: Tells `dd` to copy exactly one block.
*   `skip=N`: Tells `dd` to skip N blocks from the start of the input to reach the specific sector we want (0 for MBR, 1 for GPT Header).

**Q5: Why does a GPT formatted disk have the MBR?**  
**Answer:** It contains a "Protective MBR" at LBA 0. This exists for backward compatibility to protect the GPT data from legacy disk utilities (which don't understand GPT) so they don't see the disk as empty and overwrite it.

**Q6: Name two differences between primary and logical partitions in an MBR partitioning scheme.**  
**Answer:**
1.  There is a limit of 4 primary partitions, whereas logical partitions reside inside an Extended partition and can be much more numerous.
2.  Operating systems generally boot directly from primary partitions (marked active), while booting from logical partitions usually requires a chain-loader.

### 3. UEFI Booting

**Q1: Why is Shim used to load the GRUB bootloader?**  
**Answer:** Shim is a pre-bootloader signed by Microsoft's key (which comes pre-installed in most UEFI firmwares). It allows Linux distros to boot on Secure Boot-enabled systems by chaining to the GRUB bootloader (which Shim verifies using its own embedded certificate).

**Q2: Can you locate your grub configuration file? Show the path.**  
**Answer:** The main configuration file is located at `/boot/grub/grub.cfg`.

**Q3: According to the boot order, what is the third boot device on your computer? How did you check this?**  
**Answer:** I checked this using `efibootmgr -v`. In the `BootOrder` line (e.g., `0001,0002,0003`), the third ID corresponds to the third device in the list below it. (Refer to Figure 10 for the specific device, usually CD/DVD or Network).

---

## 4) References

1.  Ubuntu Manpages. [http://manpages.ubuntu.com/](http://manpages.ubuntu.com/)
2.  UEFI Specification. [https://uefi.org/specifications](https://uefi.org/specifications)
3.  ArchWiki - GUID Partition Table. [https://wiki.archlinux.org/title/GUID_Partition_Table](https://wiki.archlinux.org/title/GUID_Partition_Table)
