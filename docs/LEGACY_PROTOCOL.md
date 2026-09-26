# Legacy compatibility contract

The Simple Voice Call mod uses Simple Voice Chat groups as a lightweight signalling
transport. A compatible project should preserve this protocol.

## Phone item

The original client opens the phone screen when the player uses a custom-named
iron ingot. The display name must contain one of:

- `Телефон`
- `Phone`
- `电话`
- `Telefon`
- `電話`

Keep this detection path even if the project later adds a proper item.

## Group names

Incoming calls are represented by voice chat groups named:

```text
C<encoded target UUID>
```

Decline signals are represented by temporary voice chat groups named:

```text
D<encoded caller UUID>
```

The UUID encoding is URL-safe Base64 of the 16 raw UUID bytes with padding
removed.

## Optional password

When password-protected calls are enabled, the original uses the group name
itself as the password. A compatible project should keep accepting this.

## Local data

The original stores data in Fabric's config directory:

- `simple-voice-call.json`
- `phone-numbers.json`

A project should import these files without deleting them and should write backups
before performing migrations.

