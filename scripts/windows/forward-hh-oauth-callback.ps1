param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$CallbackUri
)

$ErrorActionPreference = "Stop"

$EndMarker = "__END__"
$DefaultForwardPort = 54347

function Resolve-ForwardPort {
    $envPort = $env:HH_CUSTOM_SCHEME_FORWARD_PORT

    if ([string]::IsNullOrWhiteSpace($envPort)) {
        return $DefaultForwardPort
    }

    $parsedPort = 0

    if (-not [int]::TryParse($envPort, [ref]$parsedPort)) {
        return $DefaultForwardPort
    }

    if ($parsedPort -lt 1024 -or $parsedPort -gt 65535) {
        return $DefaultForwardPort
    }

    return $parsedPort
}

$client = $null
$writer = $null
$reader = $null

try {
    $forwardPort = Resolve-ForwardPort

    $client = [System.Net.Sockets.TcpClient]::new()
    $client.ReceiveTimeout = 2000
    $client.SendTimeout = 2000

    $connectTask = $client.ConnectAsync("127.0.0.1", $forwardPort)

    if (-not $connectTask.Wait(1000)) {
        exit 1
    }

    $stream = $client.GetStream()

    $writer = [System.IO.StreamWriter]::new($stream, [System.Text.Encoding]::UTF8)
    $writer.NewLine = "`n"
    $writer.AutoFlush = $true

    $reader = [System.IO.StreamReader]::new($stream, [System.Text.Encoding]::UTF8)

    $writer.WriteLine($CallbackUri)
    $writer.WriteLine($EndMarker)

    $response = $reader.ReadLine()

    if ($response -eq "OK" -or $response -eq "IGNORED") {
        exit 0
    }

    exit 2
}
catch {
    exit 1
}
finally {
    if ($reader -ne $null) {
        $reader.Dispose()
    }

    if ($writer -ne $null) {
        $writer.Dispose()
    }

    if ($client -ne $null) {
        $client.Dispose()
    }
}