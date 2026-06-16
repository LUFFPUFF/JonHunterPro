param(
    [Parameter(Mandatory = $true)]
    [string]$CallbackUri
)

$ErrorActionPreference = "Stop"

$CallbackUri = $CallbackUri.Trim('"')

if ([string]::IsNullOrWhiteSpace($CallbackUri)) {
    exit 1
}

$portValue = $env:HH_CUSTOM_SCHEME_FORWARD_PORT

if ([string]::IsNullOrWhiteSpace($portValue)) {
    $port = 54347
} else {
    $port = [int]$portValue
}

$client = New-Object System.Net.Sockets.TcpClient

try {
    $client.Connect("127.0.0.1", $port)

    $stream = $client.GetStream()

    $payload = $CallbackUri + "`n__END__`n"
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($payload)

    $stream.Write($bytes, 0, $bytes.Length)
    $stream.Flush()

    $buffer = New-Object byte[] 128
    [void]$stream.Read($buffer, 0, $buffer.Length)
} finally {
    $client.Close()
}