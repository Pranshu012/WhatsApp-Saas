# Oracle Cloud Setup

Target: one ARM VM, 2 OCPU / 12 GB, Ubuntu 24.04, in Mumbai or Hyderabad.

## ⚠️ Two irreversible decisions at signup

1. **Home region cannot be changed.** Pick **India West (Mumbai)** or **India South
   (Hyderabad)**. Getting this wrong means deleting the account and starting over.
2. **Always Free limits changed on 15 June 2026** — ARM went from 4 OCPU/24 GB to
   **2 OCPU/12 GB**. Do not follow an older tutorial that provisions 4/24; it will be
   terminated.

## Signup

1. https://www.oracle.com/cloud/free/
2. Real name, email, **India** as country
3. Region: **India West (Mumbai)** or **India South (Hyderabad)** ← irreversible
4. Credit/debit card for identity verification. A small temporary hold appears and is
   reversed. Always Free resources are not charged.
5. Verify email, set password

**Upgrade to Pay As You Go?** Optional and worth considering later, not now. It reduces the
chance of idle reclamation and lets you exceed free limits deliberately. Always Free resources
stay free either way. Start on Always Free; revisit when you have revenue.

## Create the instance

**Compute → Instances → Create instance**

| Field | Value |
|---|---|
| Name | `wasaas-prod` |
| Image | Canonical Ubuntu 24.04 |
| Shape | **VM.Standard.A1.Flex** (Ampere ARM) |
| OCPUs | **2** |
| Memory | **12 GB** |
| Boot volume | 50 GB (within the 200 GB free allowance) |
| VCN | Create new, with a public subnet |
| Public IPv4 | **Assign** |
| SSH keys | Upload your public key |

**"Out of capacity" is normal.** ARM capacity in Indian regions is frequently exhausted.
Options: retry every few hours (capacity frees up), try the other Indian region, or run a
polling script against the OCI CLI. It can take a day or two. Don't switch to x86 unless you
have to — ARM gives you 12 GB free where AMD micro gives you 1 GB.

**Reserve a static public IP:** Networking → Reserved IPs. An ephemeral IP changes on stop/start
and breaks your DNS and Meta webhook URL.

## Network — Security List

Oracle's cloud firewall is separate from the OS firewall. Both must allow traffic.

**Networking → VCN → Subnet → Security List → Ingress rules:**

| Source | Protocol | Port | Purpose |
|---|---|---|---|
| `0.0.0.0/0` | TCP | 443 | HTTPS |
| `0.0.0.0/0` | TCP | 80 | HTTP (Caddy's TLS challenge + redirect) |
| `<your IP>/32` | TCP | 22 | SSH — **your IP only**, not `0.0.0.0/0` |

Do **not** open 5432. Postgres listens on localhost only.

## ⚠️ Ubuntu's iptables trap

Oracle's Ubuntu images ship with pre-configured iptables rules that **drop inbound traffic and
persist across reboots**. Your ports will appear closed even with correct Security List rules.
This wastes hours if you don't know about it.

```bash
sudo iptables -L INPUT -n --line-numbers   # inspect
```

The provisioning script (F22) handles this by installing UFW and clearing the conflicting
rules. If you're doing it by hand, remove the REJECT rules in the INPUT chain and persist,
or migrate cleanly to UFW.

## First connect

```bash
ssh -i ~/.ssh/your_key ubuntu@<reserved-ip>
sudo apt update && sudo apt upgrade -y
uname -m        # expect: aarch64
free -h         # expect: ~12 GB (11.x)
nproc           # expect: 2
```

**`aarch64` matters.** Every binary you install must be ARM64. Use Temurin JDK 21 for aarch64;
if a tool has no ARM build, find an alternative rather than emulating it.

## Idle reclamation

Oracle may reclaim Always Free instances judged idle (low CPU, network, and memory over a
7-day window). A real application with a worker polling Postgres and webhooks arriving is not
idle. Still, the provisioning script adds a lightweight cron heartbeat as insurance — cheap
protection against losing the box.

## Definition of Done

- [ ] Home region is Mumbai or Hyderabad
- [ ] Shape VM.Standard.A1.Flex, exactly 2 OCPU / 12 GB
- [ ] Reserved (static) public IP attached
- [ ] Security List: 443 and 80 open, 22 restricted to your IP, 5432 closed
- [ ] SSH works with a key; password auth never enabled
- [ ] `uname -m` → `aarch64`
- [ ] iptables is not silently dropping inbound traffic
- [ ] The IP is recorded in your password manager alongside the SSH key
