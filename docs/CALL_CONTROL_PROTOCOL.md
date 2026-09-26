# Call control protocol v1 (1.1.6)

Voice remains on the original Simple Voice Chat C/D/B/U group protocol. Ordinary
calls require no call-control backend. These endpoints add consent messages only;
they never create, join or leave a voice group on behalf of a client.

## Capability negotiation

POST `/calls/poll` once per second. JSON fields:

- `player`: Minecraft UUID; `server`: current server address in lower case.
- `secret`: private random device credential (at least 32 characters), stored
  locally and never included in number registration, resolve or public responses.
- `protocol`: integer 1.
- `capabilities`: `call_waiting_v1`, `call_transfer_v1`, only if actually implemented.
- `name`, `group` (SVC UUID or null), `state` (NONE, ACTIVE, OUTGOING_RINGING,
  INCOMING_RINGING, BUSY), `phone` (boolean).

Response: `peers` and `offers` for this server and caller. Peer capabilities are
live leases, valid for eight seconds; number registration is not capability proof.
Original clients, old Pro Max clients, unknown protocol versions, offline clients
and clients missing a feature are unavailable for that feature. A compatible
third-party client can implement protocol 1 regardless of mod name.

The first private credential binds a player/server identity (trust on first use).
This is not Minecraft account authentication. Clients independently verify the
source UUID and actual group membership through their Minecraft/SVC connection;
never join solely because an HTTP message names a group. Use HTTPS when a TLS
endpoint is configured. Existing deployments may use their existing HTTP endpoint.
An administrator can remove the player's row from `call_devices` after loss of
the private local credential; public registry endpoints do not reset it.

## Requests and state transitions

All requests include `player`, `server`, `secret`.

- POST `/calls/offer`: unique UUID `id`, `kind` (`waiting` or `transfer`),
  `target`, `group`, `anchor` for transfer. Retry with the same id is idempotent.
- POST `/calls/respond`: `id`, `decision`: accepted / declined / failed.
  Only the recipient can respond. Acceptance reserves 15 seconds for joining.
- POST `/calls/cancel`: `id`; only source. Does not switch any voice group.
- POST `/calls/complete`: `id`; only source, after accepted target heartbeat
  confirms that target joined the group. Retries are idempotent.

Offers start pending, expire after 45 seconds, and are cancelled if source/target
go offline, source group changes, or required capabilities disappear. Transfers
also require the remaining participant to stay in the original group with support.
Only one live outgoing offer per source and one live incoming offer per recipient
are allowed. Offers and presence are ephemeral and disappear on backend restart.

### Waiting

Both caller and recipient must advertise call_waiting_v1. Caller creates its normal
legacy `C<encoded recipient UUID>` group and sends the offer while ringing.
Recipient keeps its active call and shows a separate popup. Decline is HTTP-only.
After explicit acceptance and successful server acknowledgement, recipient ends
the previous call and joins the caller's verified group. Source observes the join
through SVC as with a normal original-protocol call. No automatic group merge.

### Transfer

Source, remaining participant (anchor) and recipient must all advertise
call_transfer_v1. Source must be in an active two-person call; recipient must be
free. Source sends an offer without leaving. Recipient verifies source/anchor
membership and accepts, then joins the original group using the existing SVC
password convention. Source waits for SVC membership plus backend completion
before leaving. Remaining participant updates displayed peer when source leaves.
The accepting recipient temporarily shares the group with both current players.
On failed/cancelled transfer the source stays; an already joining recipient exits.

Clients must protect callbacks with session generations, reject stale group
changes, honour blocked contacts and phone presence, and never auto-rejoin after
a disconnect. See `backend/test_call_signalling.py` and
`work/test_client_control.py` for reference verification scenarios.


## 1.1.9 extension

Capabilities: group_invite_v1, group_remove_v1, group_transfer_v1, call_hold_v1.
Poll also sends observed SVC `members`, locally tracked `created_group`, and
`hold_acks`; replies add `owners`, `removals`, and `holds`.
`offer(kind=invite)` joins an existing group only after recipient consent and never
moves the inviter out. Group transfers require the group capability from all members.
`remove` checks creator ownership, all-member compatibility, actual client-observed
roster, one pending command, and at least two remaining including the creator.
The target verifies its own SVC roster before leaving; other members retain the group.
`hold` starts preparing and waits for remaining peers to acknowledge the hold via
poll before permitting the holder to leave. Holding lasts at most 300 seconds.
Peers suppress empty-peer automatic hangup only while backend hold state is fresh.
Return is explicit, and rejoining completes the hold. `hold-release` is holder-only.
No endpoint directly mutates SVC server state. Membership/ownership are cooperative
client claims with local SVC checks, not server-admin kick authority or Mojang auth.
Selecting Legacy withdraws capabilities, invalidates async callbacks, resets the idle
transport, and delegates to the original JAR. It does not announce Yuki capabilities.
